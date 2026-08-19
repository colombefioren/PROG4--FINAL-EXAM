package org.cocojojo.mg.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.ExamRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.ExamResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.CourseAssignmentMapper;
import org.cocojojo.mg.mapper.ExamMapper;
import org.cocojojo.mg.model.CourseAssignment;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.ExamRepository;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JExam;
import org.cocojojo.mg.util.SecurityUtil;
import org.cocojojo.mg.validator.ExamValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExamService {

  private final ExamRepository examRepository;
  private final CourseAssignmentRepository courseAssignmentRepository;
  private final ExamMapper mapper;
  private final CourseAssignmentMapper courseAssignmentMapper;
  private final ExamValidator validator;
  private final SecurityUtil securityUtil;

  public List<ExamResponse> getByCourseAssignmentId(
      UUID courseAssignmentId, Instant from, Instant to) {
    var assignment =
        courseAssignmentMapper.toModel(
            courseAssignmentRepository
                .findById(courseAssignmentId)
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "CourseAssignment with id:" + courseAssignmentId + " not found.")));
    requireCanView(assignment);
    return examRepository
        .findByCourseAssignmentIdAndDateRange(courseAssignmentId, from, to)
        .stream()
        .map(mapper::toResponse)
        .toList();
  }

  public ExamResponse getById(UUID courseAssignmentId, UUID examId) {
    var entity = getExamOrThrow(examId);
    if (!entity.getCourseAssignment().getId().equals(courseAssignmentId)) {
      throw new ResourceNotFoundException("Exam with id:" + examId + " not found.");
    }
    requireCanView(courseAssignmentMapper.toModel(entity.getCourseAssignment()));
    return mapper.toResponse(entity);
  }

  @Transactional
  public ExamResponse upsert(UUID courseAssignmentId, ExamRequest request) {
    var assignmentEntity =
        courseAssignmentRepository
            .findById(courseAssignmentId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "CourseAssignment with id:" + courseAssignmentId + " not found."));
    var assignment = courseAssignmentMapper.toModel(assignmentEntity);
    requireCanManage(assignment);
    validator.validateCoefficient(courseAssignmentId, request.id(), request.coefficient());

    var entity =
        request.id() == null
            ? newExam(request, assignmentEntity)
            : updateExam(request, assignmentEntity);
    return mapper.toResponse(examRepository.save(entity));
  }

  @Transactional
  public void delete(UUID courseAssignmentId, UUID examId) {
    var entity = getExamOrThrow(examId);
    if (!entity.getCourseAssignment().getId().equals(courseAssignmentId)) {
      throw new ResourceNotFoundException("Exam with id:" + examId + " not found.");
    }
    requireCanManage(courseAssignmentMapper.toModel(entity.getCourseAssignment()));
    examRepository.delete(entity);
  }

  private JExam newExam(ExamRequest request, JCourseAssignment assignment) {
    return mapper.toEntity(
        null, assignment, request.title(), request.examDatetime(), request.coefficient());
  }

  private JExam updateExam(ExamRequest request, JCourseAssignment assignment) {
    var entity = getExamOrThrow(request.id());
    entity.setCourseAssignment(assignment);
    entity.setTitle(request.title());
    entity.setExamDatetime(request.examDatetime());
    entity.setCoefficientFraction(request.coefficient());
    return entity;
  }

  private JExam getExamOrThrow(UUID examId) {
    return examRepository
        .findById(examId)
        .orElseThrow(() -> new ResourceNotFoundException("Exam with id:" + examId + " not found."));
  }

  private void requireCanView(CourseAssignment assignment) {
    if (securityUtil.isAdmin()) {
      return;
    }
    if (securityUtil.isTeacher()) {
      validator.validateTeacherTeaches(securityUtil.getCurrentUserId(), assignment);
      return;
    }
    if (securityUtil.isStudent()) {
      validator.validateStudentInCurriculum(securityUtil.getCurrentUserId(), assignment);
      return;
    }
    throw new ForbiddenAccessException("Only students, teachers and admins can view exams");
  }

  private void requireCanManage(CourseAssignment assignment) {
    if (securityUtil.isAdmin()) {
      return;
    }
    if (securityUtil.isTeacher()) {
      validator.validateTeacherTeaches(securityUtil.getCurrentUserId(), assignment);
      return;
    }
    throw new ForbiddenAccessException("Only admins and teachers can manage exams");
  }
}
