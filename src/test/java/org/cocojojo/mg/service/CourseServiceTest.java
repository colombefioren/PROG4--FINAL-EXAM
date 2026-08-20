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
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ConflictException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.CourseMapper;
import org.cocojojo.mg.model.Course;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.CourseRepository;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

  @Mock private CourseRepository courseRepository;
  @Mock private CourseMapper courseMapper;
  @Mock private CourseAssignmentRepository courseAssignmentRepository;
  @Mock private SecurityUtil securityUtil;

  @InjectMocks private CourseService service;

  private UUID id;
  private JCourse jCourse;
  private Course course;
  private CourseResponse response;

  @BeforeEach
  void setUp() {
    id = UUID.randomUUID();
    jCourse =
        JCourse.builder()
            .id(id)
            .code("ALG1")
            .name("Algorithms")
            .credits(6)
            .totalHours(30)
            .studentLevel(StudentLevel.L1)
            .track(null)
            .build();
    course =
        Course.builder()
            .id(id)
            .code("ALG1")
            .name("Algorithms")
            .credits(6)
            .totalHours(30)
            .studentLevel(StudentLevel.L1)
            .track(null)
            .build();
    response =
        CourseResponse.builder()
            .id(id)
            .code("ALG1")
            .name("Algorithms")
            .credits(6)
            .totalHours(30)
            .studentLevel(StudentLevel.L1)
            .track(null)
            .build();
  }

  @Test
  void getAll_maps_paged_courses() {
    given(courseRepository.findAll(any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of(jCourse)));
    given(courseMapper.toModel(jCourse)).willReturn(course);
    given(courseMapper.toResponse(course)).willReturn(response);

    var page = service.getAll(Pageable.unpaged());

    assertEquals(1, page.getTotalElements());
    assertEquals(response, page.getContent().get(0));
  }

  @Test
  void getById_maps_found_course() {
    given(courseRepository.findById(id)).willReturn(Optional.of(jCourse));
    given(courseMapper.toModel(jCourse)).willReturn(course);
    given(courseMapper.toResponse(course)).willReturn(response);

    assertEquals(response, service.getById(id));
  }

  @Test
  void getById_throws_not_found_when_missing() {
    given(courseRepository.findById(id)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.getById(id));
  }

  @Test
  void upsert_creates_new_course_with_uppercased_code() {
    var request =
        CourseRequest.builder()
            .code("alg1")
            .name("Algorithms")
            .credits(6)
            .totalHours(30)
            .studentLevel(StudentLevel.L1)
            .track(Track.TN)
            .build();

    given(courseRepository.save(any(JCourse.class))).willReturn(jCourse);
    given(courseMapper.toModel(jCourse)).willReturn(course);
    given(courseMapper.toResponse(course)).willReturn(response);

    service.upsert(request);

    ArgumentCaptor<JCourse> captor = ArgumentCaptor.forClass(JCourse.class);
    then(courseRepository).should().save(captor.capture());
    assertEquals("ALG1", captor.getValue().getCode());
  }

  @Test
  void upsert_updates_existing_course() {
    var request =
        CourseRequest.builder()
            .id(id)
            .code("ALG1")
            .name("Algorithms SL")
            .credits(4)
            .totalHours(20)
            .studentLevel(StudentLevel.L1)
            .track(null)
            .build();

    given(courseRepository.findById(id)).willReturn(Optional.of(jCourse));
    given(courseRepository.save(jCourse)).willReturn(jCourse);
    given(courseMapper.toModel(jCourse)).willReturn(course);
    given(courseMapper.toResponse(course)).willReturn(response);

    service.upsert(request);

    then(courseRepository).should().save(jCourse);
  }

  @Test
  void getByStudentLevelOrderByCodeAsc_returns_mapped_list() {
    given(courseRepository.findByStudentLevelOrderByCodeAsc(StudentLevel.L1))
        .willReturn(List.of(jCourse));
    given(courseMapper.toModel(jCourse)).willReturn(course);

    var result = service.getByStudentLevelOrderByCodeAsc(StudentLevel.L1);

    assertEquals(1, result.size());
    assertEquals(course, result.get(0));
  }

  @Test
  void delete_removes_course_for_admin_when_unassigned() {
    given(securityUtil.isAdmin()).willReturn(true);
    given(courseRepository.findById(id)).willReturn(Optional.of(jCourse));
    given(courseAssignmentRepository.existsByCourseId(id)).willReturn(false);

    service.delete(id);

    then(courseRepository).should().delete(jCourse);
  }

  @Test
  void delete_rejects_non_admin() {
    given(securityUtil.isAdmin()).willReturn(false);

    assertThrows(ForbiddenAccessException.class, () -> service.delete(id));
    then(courseRepository).should(never()).delete(any());
  }

  @Test
  void delete_rejects_assigned_course() {
    given(securityUtil.isAdmin()).willReturn(true);
    given(courseRepository.findById(id)).willReturn(Optional.of(jCourse));
    given(courseAssignmentRepository.existsByCourseId(id)).willReturn(true);

    assertThrows(ConflictException.class, () -> service.delete(id));
    then(courseRepository).should(never()).delete(any());
  }

  @Test
  void delete_throws_not_found_when_missing() {
    given(securityUtil.isAdmin()).willReturn(true);
    given(courseRepository.findById(id)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.delete(id));
  }
}
