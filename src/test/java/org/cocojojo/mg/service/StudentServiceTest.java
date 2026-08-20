package org.cocojojo.mg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.StudentRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.StudentResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.GroupMapper;
import org.cocojojo.mg.mapper.StudentMapper;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.model.Promotion;
import org.cocojojo.mg.model.Student;
import org.cocojojo.mg.repository.GradeHistoryRepository;
import org.cocojojo.mg.repository.GradeRepository;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.util.SecurityUtil;
import org.cocojojo.mg.util.StdRefGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

  @Mock private StudentRepository repository;
  @Mock private StudentMapper mapper;
  @Mock private GroupMapper groupMapper;
  @Mock private GroupService groupService;
  @Mock private GroupFlowService groupFlowService;
  @Mock private StdRefGenerator stdRefGenerator;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private SecurityUtil securityUtil;
  @Mock private GradeRepository gradeRepository;
  @Mock private GradeHistoryRepository gradeHistoryRepository;
  @Mock private GroupFlowRepository groupFlowRepository;

  @InjectMocks private StudentService service;

  private UUID id;
  private UUID groupId;
  private UUID promotionId;
  private JStudent entity;
  private JGroup jGroup;
  private Group group;
  private Student model;
  private StudentResponse response;

  @BeforeEach
  void setUp() {
    id = UUID.randomUUID();
    groupId = UUID.randomUUID();
    promotionId = UUID.randomUUID();
    var jPromotion =
        JPromotion.builder().id(promotionId).ref("P1").name("Promotion").entryYear(2023).build();
    entity =
        JStudent.builder()
            .id(id)
            .firstname("Alan")
            .lastname("Turing")
            .email("alan@hei.school")
            .password("secret")
            .std("STD-01")
            .promotion(jPromotion)
            .build();
    jGroup = JGroup.builder().id(groupId).ref("G1").promotion(jPromotion).build();
    group =
        Group.builder()
            .id(groupId)
            .ref("G1")
            .promotion(
                Promotion.builder()
                    .id(promotionId)
                    .ref("P1")
                    .name("Promotion")
                    .entryYear(2023)
                    .build())
            .build();
    model =
        Student.builder()
            .id(id)
            .firstname("Alan")
            .lastname("Turing")
            .email("alan@hei.school")
            .password("secret")
            .std("STD-01")
            .promotion(
                Promotion.builder()
                    .id(promotionId)
                    .ref("P1")
                    .name("Promotion")
                    .entryYear(2023)
                    .build())
            .currentGroup(group)
            .build();
    response =
        StudentResponse.builder()
            .id(id)
            .std("STD-01")
            .firstname("Alan")
            .lastname("Turing")
            .email("alan@hei.school")
            .promotionId(promotionId)
            .promotionName("Promotion")
            .currentGroupId(groupId)
            .currentGroupRef("G1")
            .build();
  }

  @Test
  void getAll_maps_paged_students() {
    given(repository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(entity)));
    given(groupFlowService.findCurrentGroup(id)).willReturn(Optional.of(jGroup));
    given(mapper.toModel(any(), any())).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    var page = service.getAll(Pageable.unpaged());

    assertEquals(1, page.getTotalElements());
    assertEquals(response, page.getContent().get(0));
  }

  @Test
  void getById_requires_self_or_staff() {
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(groupFlowService.findCurrentGroup(id)).willReturn(Optional.of(jGroup));
    given(mapper.toModel(any(), any())).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    assertEquals(response, service.getById(id));
  }

  @Test
  void getById_throws_when_missing() {
    given(repository.findById(id)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.getById(id));
  }

  @Test
  void getEntityOrThrow_returns_found() {
    given(repository.findById(id)).willReturn(Optional.of(entity));

    assertEquals(entity, service.getEntityOrThrow(id));
  }

  @Test
  void getEntityOrThrow_throws_when_missing() {
    given(repository.findById(id)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.getEntityOrThrow(id));
  }

  @Test
  void getByGroup_returns_current_members() {
    given(groupService.getEntityOrThrow(groupId)).willReturn(jGroup);
    given(groupFlowService.getCurrentStudentIdsInGroup(groupId)).willReturn(List.of(id));
    given(repository.findAllById(List.of(id))).willReturn(List.of(entity));
    given(groupFlowService.findCurrentGroup(id)).willReturn(Optional.of(jGroup));
    given(mapper.toModel(any(), any())).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    var result = service.getByGroup(groupId);

    assertEquals(1, result.size());
    assertEquals(response, result.get(0));
  }

  @Test
  void getCurrentGroup_throws_when_no_group() {
    given(groupFlowService.getCurrentGroup(id))
        .willThrow(new ResourceNotFoundException("Student with id:" + id + " has no group"));

    assertThrows(ResourceNotFoundException.class, () -> service.getCurrentGroup(id));
  }

  @Test
  void create_requires_group_id() {
    var request =
        StudentRequest.builder()
            .firstname("Alan")
            .lastname("Turing")
            .email("alan@hei.school")
            .build();

    assertThrows(IllegalArgumentException.class, () -> service.upsert(request));
  }

  @Test
  void create_requires_password() {
    var request =
        StudentRequest.builder()
            .firstname("Alan")
            .lastname("Turing")
            .email("alan@hei.school")
            .groupId(groupId)
            .build();

    assertThrows(IllegalArgumentException.class, () -> service.upsert(request));
  }

  @Test
  void create_saves_and_joins_group() {
    var request =
        StudentRequest.builder()
            .firstname("Alan")
            .lastname("Turing")
            .email("alan@hei.school")
            .password("secret")
            .groupId(groupId)
            .build();
    given(groupService.getEntityOrThrow(groupId)).willReturn(jGroup);
    given(stdRefGenerator.generate(2023)).willReturn("STD-01");
    given(passwordEncoder.encode("secret")).willReturn("encoded");
    given(repository.save(any(JStudent.class))).willReturn(entity);
    given(groupFlowService.findCurrentGroup(id)).willReturn(Optional.of(jGroup));
    given(mapper.toModel(any(), any())).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    var result = service.upsert(request);

    assertEquals(response, result);
    then(groupFlowService).should().join(any(JStudent.class), any(JGroup.class));
  }

  @Test
  void update_requires_no_group_id_on_update() {
    var request =
        StudentRequest.builder()
            .id(id)
            .firstname("Alan")
            .lastname("Turing")
            .email("alan@hei.school")
            .groupId(groupId)
            .build();

    assertThrows(IllegalArgumentException.class, () -> service.upsert(request));
  }

  @Test
  void update_keeps_password_when_blank() {
    var request =
        StudentRequest.builder()
            .id(id)
            .firstname("Alan")
            .lastname("Turing")
            .email("alan@hei.school")
            .password("")
            .build();
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(repository.save(entity)).willReturn(entity);
    given(groupFlowService.findCurrentGroup(id)).willReturn(Optional.of(jGroup));
    given(mapper.toModel(any(), any())).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    service.upsert(request);

    then(passwordEncoder).should(never()).encode(any());
  }

  @Test
  void update_re_encodes_password_when_provided() {
    var request =
        StudentRequest.builder()
            .id(id)
            .firstname("Alan")
            .lastname("Turing")
            .email("alan@hei.school")
            .password("newSecret")
            .build();
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(passwordEncoder.encode("newSecret")).willReturn("newEncoded");
    given(repository.save(entity)).willReturn(entity);
    given(groupFlowService.findCurrentGroup(id)).willReturn(Optional.of(jGroup));
    given(mapper.toModel(any(), any())).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    service.upsert(request);

    then(passwordEncoder).should().encode("newSecret");
  }

  @Test
  void delete_requires_admin() {
    given(securityUtil.isAdmin()).willReturn(false);

    assertThrows(ForbiddenAccessException.class, () -> service.delete(id));
    then(repository).should(never()).softDeleteById(any());
  }

  @Test
  void delete_purges_grades_and_flows_and_soft_deletes() {
    given(securityUtil.isAdmin()).willReturn(true);
    given(repository.findById(id)).willReturn(Optional.of(entity));
    var jGrade = org.cocojojo.mg.repository.model.JGrade.builder().id(UUID.randomUUID()).build();
    given(gradeRepository.findByStudentId(id)).willReturn(List.of(jGrade));

    service.delete(id);

    then(gradeRepository).should().deleteAll(List.of(jGrade));
    then(repository).should().softDeleteById(id);
  }
}
