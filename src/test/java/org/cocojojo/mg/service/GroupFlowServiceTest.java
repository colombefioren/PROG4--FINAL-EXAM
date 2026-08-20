package org.cocojojo.mg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupFlowResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.MoveStudentGroupRequest;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.GroupFlowMapper;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.model.GroupFlow;
import org.cocojojo.mg.model.StudentSummary;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JGroupFlow;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.validator.GroupFlowValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupFlowServiceTest {

  @Mock private GroupFlowRepository repository;
  @Mock private StudentRepository studentRepository;
  @Mock private GroupFlowMapper mapper;
  @Mock private GroupFlowValidator validator;
  @Mock private GroupService groupService;

  @InjectMocks private GroupFlowService service;

  private UUID flowId;
  private UUID studentId;
  private UUID groupId;
  private JStudent jStudent;
  private JGroup jGroup;
  private JGroupFlow jFlow;
  private GroupFlow model;
  private GroupFlowResponse response;

  @BeforeEach
  void setUp() {
    flowId = UUID.randomUUID();
    studentId = UUID.randomUUID();
    groupId = UUID.randomUUID();
    jStudent = JStudent.builder().id(studentId).firstname("Alan").lastname("Turing").build();
    jGroup = JGroup.builder().id(groupId).ref("G1").build();
    jFlow =
        JGroupFlow.builder()
            .id(flowId)
            .student(jStudent)
            .group(jGroup)
            .groupFlowType(GroupFlowType.JOIN)
            .createdAt(Instant.parse("2024-06-01T08:00:00Z"))
            .build();
    model =
        GroupFlow.builder()
            .id(flowId)
            .student(StudentSummary.builder().id(studentId).std("STD-01").build())
            .group(Group.builder().id(groupId).ref("G1").build())
            .groupFlowType(GroupFlowType.JOIN)
            .createdAt(Instant.parse("2024-06-01T08:00:00Z"))
            .build();
    response =
        GroupFlowResponse.builder()
            .id(flowId)
            .studentId(studentId)
            .studentStd("STD-01")
            .groupId(groupId)
            .groupRef("G1")
            .groupFlowType(GroupFlowType.JOIN)
            .createdAt(Instant.parse("2024-06-01T08:00:00Z"))
            .build();
  }

  @Test
  void getHistory_returns_mapped_flows() {
    given(repository.findByStudentIdOrderByCreatedAtDesc(studentId)).willReturn(List.of(jFlow));
    given(mapper.toModel(jFlow)).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    var result = service.getHistory(studentId);

    assertEquals(1, result.size());
    assertEquals(response, result.get(0));
  }

  @Test
  void findCurrentGroup_returns_join_group() {
    given(repository.findFirstByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(Optional.of(jFlow));

    var group = service.findCurrentGroup(studentId);

    assertTrue(group.isPresent());
    assertEquals(groupId, group.get().getId());
  }

  @Test
  void findCurrentGroup_is_empty_when_leave_flow() {
    var leaveFlow =
        JGroupFlow.builder()
            .id(flowId)
            .student(jStudent)
            .group(jGroup)
            .groupFlowType(GroupFlowType.LEAVE)
            .build();
    given(repository.findFirstByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(Optional.of(leaveFlow));

    var group = service.findCurrentGroup(studentId);

    assertTrue(group.isEmpty());
  }

  @Test
  void getCurrentGroup_returns_join_group() {
    given(repository.findFirstByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(Optional.of(jFlow));

    var group = service.getCurrentGroup(studentId);

    assertEquals(groupId, group.getId());
  }

  @Test
  void getCurrentGroup_throws_when_no_group() {
    given(repository.findFirstByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.getCurrentGroup(studentId));
  }

  @Test
  void getCurrentStudentIdsInGroup_filters_join_only() {
    var other =
        JStudent.builder().id(UUID.randomUUID()).firstname("Grace").lastname("Hopper").build();
    var otherFlow =
        JGroupFlow.builder()
            .id(UUID.randomUUID())
            .student(other)
            .group(jGroup)
            .groupFlowType(GroupFlowType.JOIN)
            .build();
    given(repository.findByGroupId(groupId)).willReturn(List.of(jFlow, otherFlow));
    given(repository.findFirstByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(Optional.of(jFlow));
    given(repository.findFirstByStudentIdOrderByCreatedAtDesc(other.getId()))
        .willReturn(Optional.of(otherFlow));

    var result = service.getCurrentStudentIdsInGroup(groupId);

    assertEquals(2, result.size());
    assertTrue(result.contains(studentId));
    assertTrue(result.contains(other.getId()));
  }

  @Test
  void getCurrentStudentIdsInGroup_excludes_student_currently_elsewhere() {
    var currentFlow =
        JGroupFlow.builder()
            .id(UUID.randomUUID())
            .student(jStudent)
            .group(JGroup.builder().id(UUID.randomUUID()).ref("G2").build())
            .groupFlowType(GroupFlowType.JOIN)
            .build();
    given(repository.findByGroupId(groupId)).willReturn(List.of(jFlow));
    given(repository.findFirstByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(Optional.of(currentFlow));

    var result = service.getCurrentStudentIdsInGroup(groupId);

    assertTrue(result.isEmpty());
  }

  @Test
  void join_saves_join_flow() {
    service.join(jStudent, jGroup);

    then(repository).should().save(any(JGroupFlow.class));
  }

  @Test
  void move_maps_and_updates_student_promotion() {
    given(groupService.getEntityOrThrow(groupId)).willReturn(jGroup);
    given(repository.save(any(JGroupFlow.class))).willReturn(jFlow);
    given(mapper.toModel(jFlow)).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    var result = service.move(jStudent, new MoveStudentGroupRequest(groupId));

    assertEquals(response, result);
    then(studentRepository).should().save(jStudent);
  }

  @Test
  void move_rejects_invalid_student_before_saving() {
    var request = new MoveStudentGroupRequest(groupId);
    org.mockito.BDDMockito.willThrow(new IllegalArgumentException("only students can be moved"))
        .given(validator)
        .validateIsStudent(jStudent);

    assertThrows(IllegalArgumentException.class, () -> service.move(jStudent, request));
    then(repository).should(never()).save(any(JGroupFlow.class));
  }
}
