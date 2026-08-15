package org.cocojojo.mg.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeCorrectionRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeHistoryResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.CourseAssignmentMapper;
import org.cocojojo.mg.mapper.GradeHistoryMapper;
import org.cocojojo.mg.mapper.GradeMapper;
import org.cocojojo.mg.model.CourseAssignment;
import org.cocojojo.mg.repository.ExamRepository;
import org.cocojojo.mg.repository.GradeHistoryRepository;
import org.cocojojo.mg.repository.GradeRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.UserRepository;
import org.cocojojo.mg.repository.model.JExam;
import org.cocojojo.mg.repository.model.JGrade;
import org.cocojojo.mg.repository.model.JUser;
import org.cocojojo.mg.util.SecurityUtil;
import org.cocojojo.mg.validator.GradeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GradeService {

  private final GradeRepository gradeRepository;
  private final GradeHistoryRepository gradeHistoryRepository;
  private final ExamRepository examRepository;
  private final StudentRepository studentRepository;
  private final UserRepository userRepository;
  private final GradeMapper mapper;
  private final GradeHistoryMapper historyMapper;
  private final CourseAssignmentMapper courseAssignmentMapper;
  private final GradeValidator validator;
  private final SecurityUtil securityUtil;

  public List<GradeResponse> getByExamId(UUID examId) {
    var exam = getExam(examId);
    requireCanManageExam(courseAssignmentMapper.toModel(exam.getCourseAssignment()));
    return gradeRepository.findByExamId(examId).stream().map(mapper::toResponse).toList();
  }

  public GradeResponse getByExamIdAndStudentId(UUID examId, UUID studentId) {
    var exam = getExam(examId);
    requireCanManageExam(courseAssignmentMapper.toModel(exam.getCourseAssignment()));
    var entity =
        gradeRepository
            .findByExamIdAndStudentId(examId, studentId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Grade not found for exam " + examId + " and student " + studentId));
    return mapper.toResponse(entity);
  }

  @Transactional
  public List<GradeResponse> upsert(UUID examId, List<GradeRequest> requests) {
    requests.forEach(r -> validator.validateExamMatchesPath(examId, r.examId()));
    var exam = getExam(examId);
    requireCanManageExam(courseAssignmentMapper.toModel(exam.getCourseAssignment()));
    var changedBy = getCurrentUser();
    return requests.stream().map(r -> upsertOne(r, exam, changedBy)).toList();
  }

  @Transactional
  public GradeResponse correct(UUID examId, UUID studentId, GradeCorrectionRequest request) {
    var exam = getExam(examId);
    requireCanManageExam(courseAssignmentMapper.toModel(exam.getCourseAssignment()));
    var entity =
        gradeRepository
            .findByExamIdAndStudentId(examId, studentId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Grade not found for exam " + examId + " and student " + studentId));
    recordHistory(entity, entity.getValue(), request.value(), request.reason(), getCurrentUser());
    entity.setValue(request.value());
    return mapper.toResponse(entity);
  }

  public List<GradeResponse> getByStudentId(UUID studentId) {
    if (securityUtil.isStudent()) {
      validator.validateIsStudentSelf(studentId);
    }
    var grades = gradeRepository.findByStudentId(studentId);
    if (securityUtil.isTeacher()) {
      var teacherId = securityUtil.getCurrentUserIdOrThrow();
      grades =
          grades.stream()
              .filter(
                  g ->
                      g.getExam().getCourseAssignment().getTeachers().stream()
                          .anyMatch(t -> t.getId().equals(teacherId)))
              .toList();
    }
    return grades.stream().map(mapper::toResponse).toList();
  }

  public GradeResponse getById(UUID gradeId) {
    var entity = getGrade(gradeId);
    if (securityUtil.isAdmin()) {
      return mapper.toResponse(entity);
    }
    if (securityUtil.isTeacher()) {
      validator.validateTeacherTeaches(
          securityUtil.getCurrentUserIdOrThrow(),
          courseAssignmentMapper.toModel(entity.getExam().getCourseAssignment()));
    }
    if (securityUtil.isStudent()) {
      validator.validateStudentOwnsGrade(
          securityUtil.getCurrentUserIdOrThrow(), mapper.toModel(entity));
    }
    return mapper.toResponse(entity);
  }

  @Transactional
  public void delete(UUID gradeId) {
    var entity = getGrade(gradeId);
    requireCanManageExam(courseAssignmentMapper.toModel(entity.getExam().getCourseAssignment()));
    gradeRepository.delete(entity);
  }

  public List<GradeHistoryResponse> getHistory(UUID gradeId) {
    var entity = getGrade(gradeId);
    if (securityUtil.isAdmin()) {
      return history(entity);
    }
    if (securityUtil.isTeacher()) {
      validator.validateTeacherTeaches(
          securityUtil.getCurrentUserIdOrThrow(),
          courseAssignmentMapper.toModel(entity.getExam().getCourseAssignment()));
      return history(entity);
    }
    throw new ForbiddenAccessException("Only teachers and admins can view grade history");
  }

  private GradeResponse upsertOne(GradeRequest request, JExam exam, JUser changedBy) {
    var student =
        studentRepository
            .findById(request.studentId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Student with id:" + request.studentId() + " not found."));
    var existing = gradeRepository.findByExamIdAndStudentId(exam.getId(), request.studentId());
    if (existing.isPresent()) {
      var entity = existing.get();
      recordHistory(entity, entity.getValue(), request.value(), "Grade updated", changedBy);
      entity.setValue(request.value());
      entity.setComment(request.comment());
      return mapper.toResponse(entity);
    }
    var entity =
        gradeRepository.save(
            mapper.toEntity(null, student, exam, request.value(), request.comment()));
    recordHistory(entity, null, request.value(), "Grade recorded", changedBy);
    return mapper.toResponse(entity);
  }

  private List<GradeHistoryResponse> history(JGrade grade) {
    return gradeHistoryRepository.findByGradeIdOrderByChangedAtDesc(grade.getId()).stream()
        .map(historyMapper::toResponse)
        .toList();
  }

  private void recordHistory(
      JGrade grade, BigDecimal previousValue, BigDecimal newValue, String reason, JUser changedBy) {
    gradeHistoryRepository.save(
        historyMapper.toEntity(null, grade, previousValue, newValue, reason, changedBy));
  }

  private JExam getExam(UUID id) {
    return examRepository
        .findWithDetailsById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Exam with id:" + id + " not found."));
  }

  private JGrade getGrade(UUID id) {
    return gradeRepository
        .findWithDetailsById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Grade with id:" + id + " not found."));
  }

  private JUser getCurrentUser() {
    return userRepository
        .findById(securityUtil.getCurrentUserIdOrThrow())
        .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
  }

  private void requireCanManageExam(CourseAssignment assignment) {
    if (securityUtil.isAdmin()) {
      return;
    }
    if (securityUtil.isTeacher()) {
      validator.validateTeacherTeaches(securityUtil.getCurrentUserIdOrThrow(), assignment);
      return;
    }
    throw new ForbiddenAccessException("Only teachers and admins can manage grades");
  }
}
