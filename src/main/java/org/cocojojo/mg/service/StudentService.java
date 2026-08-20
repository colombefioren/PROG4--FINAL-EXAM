package org.cocojojo.mg.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.StudentRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.StudentResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.GroupMapper;
import org.cocojojo.mg.mapper.StudentMapper;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.repository.GradeHistoryRepository;
import org.cocojojo.mg.repository.GradeRepository;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.model.JGrade;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.util.SecurityUtil;
import org.cocojojo.mg.util.StdRefGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentService {
  private final StudentRepository repository;
  private final StudentMapper mapper;
  private final GroupMapper groupMapper;
  private final GroupService groupService;
  private final GroupFlowService groupFlowService;
  private final StdRefGenerator stdRefGenerator;
  private final PasswordEncoder passwordEncoder;
  private final SecurityUtil securityUtil;
  private final GradeRepository gradeRepository;
  private final GradeHistoryRepository gradeHistoryRepository;
  private final GroupFlowRepository groupFlowRepository;

  public Page<StudentResponse> getAll(Pageable pageable) {
    return repository.findAll(pageable).map(this::toResponse);
  }

  public StudentResponse getById(UUID id) {
    securityUtil.requireSelfOrStaff(id);
    return toResponse(getEntityOrThrow(id));
  }

  public void assertAdminOrSelf(UUID studentId) {
    securityUtil.requireSelfOrAdmin(studentId);
  }

  public JStudent getEntityOrThrow(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Student with id: " + id + " not found."));
  }

  public List<StudentResponse> getByGroup(UUID groupId) {
    groupService.getEntityOrThrow(groupId);
    var ids = groupFlowService.getCurrentStudentIdsInGroup(groupId);
    return repository.findAllById(ids).stream().map(this::toResponse).toList();
  }

  public Group getCurrentGroup(UUID studentId) {
    return groupMapper.toModel(groupFlowService.getCurrentGroup(studentId));
  }

  @Transactional
  public StudentResponse upsert(StudentRequest request) {
    if (request.id() == null) {
      return create(request);
    }
    return update(request);
  }

  private StudentResponse create(StudentRequest request) {
    if (request.groupId() == null) {
      throw new IllegalArgumentException("groupId is required when creating a student");
    }
    if (request.password() == null || request.password().isBlank()) {
      throw new IllegalArgumentException("password is required when creating a student");
    }
    var group = groupService.getEntityOrThrow(request.groupId());
    var stdRef = stdRefGenerator.generate(group.getPromotion().getEntryYear());

    var student =
        repository.save(
            JStudent.builder()
                .firstname(request.firstname())
                .lastname(request.lastname())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .std(stdRef)
                .promotion(group.getPromotion())
                .build());

    groupFlowService.join(student, group);

    return toResponse(student);
  }

  private StudentResponse update(StudentRequest request) {
    if (request.groupId() != null) {
      throw new IllegalArgumentException(
          "groupId cannot be changed on update; moves must go through /students/{id}/group-flows");
    }
    var student = getEntityOrThrow(request.id());
    student.setFirstname(request.firstname());
    student.setLastname(request.lastname());
    student.setEmail(request.email());
    if (request.password() != null && !request.password().isBlank()) {
      student.setPassword(passwordEncoder.encode(request.password()));
    }

    return toResponse(repository.save(student));
  }

  @Transactional
  public void delete(UUID id) {
    if (!securityUtil.isAdmin()) {
      throw new ForbiddenAccessException("Only an admin can delete a student");
    }
    var student = getEntityOrThrow(id);
    var grades = gradeRepository.findByStudentId(id);
    gradeHistoryRepository.deleteAll(
        gradeHistoryRepository.findByGradeIdIn(grades.stream().map(JGrade::getId).toList()));
    gradeRepository.deleteAll(grades);
    groupFlowRepository.deleteAll(groupFlowRepository.findByStudentId(id));
    repository.softDeleteById(student.getId());
  }

  private StudentResponse toResponse(JStudent entity) {
    var currentGroup = groupFlowService.findCurrentGroup(entity.getId()).orElse(null);
    return mapper.toResponse(
        mapper.toModel(entity, currentGroup == null ? null : groupMapper.toModel(currentGroup)));
  }
}
