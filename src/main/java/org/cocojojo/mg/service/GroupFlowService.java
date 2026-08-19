package org.cocojojo.mg.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupFlowResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.MoveStudentGroupRequest;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.GroupFlowMapper;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JGroupFlow;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.validator.GroupFlowValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupFlowService {
  private final GroupFlowRepository repository;
  private final StudentRepository studentRepository;
  private final GroupFlowMapper mapper;
  private final GroupFlowValidator validator;
  private final GroupService groupService;

  public List<GroupFlowResponse> getHistory(UUID studentId) {
    return repository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
        .map(mapper::toModel)
        .map(mapper::toResponse)
        .toList();
  }

  public Optional<JGroup> findCurrentGroup(UUID studentId) {
    return repository
        .findFirstByStudentIdOrderByCreatedAtDesc(studentId)
        .filter(gf -> gf.getGroupFlowType() == GroupFlowType.JOIN)
        .map(JGroupFlow::getGroup);
  }

  public JGroup getCurrentGroup(UUID studentId) {
    return findCurrentGroup(studentId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Student with id:" + studentId + " has no group"));
  }

  public List<UUID> getCurrentStudentIdsInGroup(UUID groupId) {
    return repository.findByGroupId(groupId).stream()
        .map(flow -> flow.getStudent().getId())
        .distinct()
        .filter(
            studentId ->
                findCurrentGroup(studentId)
                    .map(group -> group.getId().equals(groupId))
                    .orElse(false))
        .toList();
  }

  @Transactional
  public void join(JStudent student, JGroup group) {
    repository.save(
        JGroupFlow.builder()
            .student(student)
            .group(group)
            .groupFlowType(GroupFlowType.JOIN)
            .build());
  }

  @Transactional
  public GroupFlowResponse move(JStudent student, MoveStudentGroupRequest request) {
    validator.validateIsStudent(student);

    var group = groupService.getEntityOrThrow(request.groupId());
    validator.validateTrackGroupSwitch(student, group);
    var saved =
        repository.save(
            JGroupFlow.builder()
                .student(student)
                .group(group)
                .groupFlowType(GroupFlowType.JOIN)
                .build());

    student.setPromotion(group.getPromotion());
    studentRepository.save(student);

    return mapper.toResponse(mapper.toModel(saved));
  }
}
