package org.cocojojo.mg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.TeacherRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.TeacherResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ConflictException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.TeacherMapper;
import org.cocojojo.mg.model.Teacher;
import org.cocojojo.mg.repository.GradeHistoryRepository;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JTeacher;
import org.cocojojo.mg.util.SecurityUtil;
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
class TeacherServiceTest {

  @Mock private TeacherRepository repository;
  @Mock private GradeHistoryRepository gradeHistoryRepository;
  @Mock private TeacherMapper mapper;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private SecurityUtil securityUtil;

  @InjectMocks private TeacherService service;

  private UUID id;
  private JTeacher entity;
  private Teacher model;
  private TeacherResponse response;

  @BeforeEach
  void setUp() {
    id = UUID.randomUUID();
    entity =
        JTeacher.builder()
            .id(id)
            .firstname("Grace")
            .lastname("Hopper")
            .email("grace@hei.school")
            .password("secret")
            .build();
    model =
        Teacher.builder()
            .id(id)
            .firstname("Grace")
            .lastname("Hopper")
            .email("grace@hei.school")
            .password("secret")
            .build();
    response =
        TeacherResponse.builder()
            .id(id)
            .firstname("Grace")
            .lastname("Hopper")
            .email("grace@hei.school")
            .build();
  }

  @Test
  void getAll_maps_paged_teachers() {
    given(repository.findAll(any(Pageable.class)))
        .willReturn(new PageImpl<>(java.util.List.of(entity)));
    given(mapper.toModel(entity)).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    var page = service.getAll(Pageable.unpaged());

    assertEquals(1, page.getTotalElements());
    assertEquals(response, page.getContent().get(0));
  }

  @Test
  void getById_maps_found_teacher() {
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(mapper.toModel(entity)).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    assertEquals(response, service.getById(id));
  }

  @Test
  void getById_throws_when_missing() {
    given(repository.findById(id)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.getById(id));
  }

  @Test
  void getEntityOrThrow_throws_when_missing() {
    given(repository.findById(id)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.getEntityOrThrow(id));
  }

  @Test
  void create_requires_password() {
    var request =
        TeacherRequest.builder()
            .firstname("Grace")
            .lastname("Hopper")
            .email("grace@hei.school")
            .build();

    assertThrows(IllegalArgumentException.class, () -> service.upsert(request));
  }

  @Test
  void create_saves_encoded_password() {
    var request =
        TeacherRequest.builder()
            .firstname("Grace")
            .lastname("Hopper")
            .email("grace@hei.school")
            .password("secret")
            .build();
    given(passwordEncoder.encode("secret")).willReturn("encoded");
    given(repository.save(any(JTeacher.class))).willReturn(entity);
    given(mapper.toModel(entity)).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    var result = service.upsert(request);

    assertEquals(response, result);
    then(passwordEncoder).should().encode("secret");
  }

  @Test
  void update_keeps_password_when_blank() {
    var request =
        TeacherRequest.builder()
            .id(id)
            .firstname("Grace")
            .lastname("Hopper")
            .email("grace@hei.school")
            .password("")
            .build();
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(repository.save(entity)).willReturn(entity);
    given(mapper.toModel(entity)).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    service.upsert(request);

    then(passwordEncoder).should(never()).encode(any());
  }

  @Test
  void update_re_encodes_password_when_provided() {
    var request =
        TeacherRequest.builder()
            .id(id)
            .firstname("Grace")
            .lastname("Hopper")
            .email("grace@hei.school")
            .password("newSecret")
            .build();
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(passwordEncoder.encode("newSecret")).willReturn("newEncoded");
    given(repository.save(entity)).willReturn(entity);
    given(mapper.toModel(entity)).willReturn(model);
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
  void delete_rejects_teacher_with_grade_changes() {
    given(securityUtil.isAdmin()).willReturn(true);
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(gradeHistoryRepository.existsByChangedById(id)).willReturn(true);

    assertThrows(ConflictException.class, () -> service.delete(id));
    then(repository).should(never()).softDeleteById(any());
  }

  @Test
  void delete_soft_deletes_teacher_without_history() {
    given(securityUtil.isAdmin()).willReturn(true);
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(gradeHistoryRepository.existsByChangedById(id)).willReturn(false);

    service.delete(id);

    then(repository).should().softDeleteById(id);
  }

  @Test
  void delete_throws_not_found_when_missing() {
    given(securityUtil.isAdmin()).willReturn(true);
    given(repository.findById(id)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.delete(id));
  }
}
