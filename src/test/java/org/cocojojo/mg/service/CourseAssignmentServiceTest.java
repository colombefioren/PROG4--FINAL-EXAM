package org.cocojojo.mg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.CurriculumStatusResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ConflictException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.CourseAssignmentMapper;
import org.cocojojo.mg.mapper.CourseMapper;
import org.cocojojo.mg.mapper.GroupMapper;
import org.cocojojo.mg.model.Course;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.ExamRepository;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JExam;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JTeacher;
import org.cocojojo.mg.util.SecurityUtil;
import org.cocojojo.mg.validator.CourseAssignmentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CourseAssignmentServiceTest {

  @Mock private CourseAssignmentRepository repository;
  @Mock private ExamRepository examRepository;
  @Mock private CourseService courseService;
  @Mock private TeacherService teacherService;
  @Mock private GroupService groupService;
  @Mock private GroupFlowService groupFlowService;
  @Mock private CourseAssignmentMapper mapper;
  @Mock private CourseMapper courseMapper;
  @Mock private GroupMapper groupMapper;
  @Mock private CourseAssignmentValidator validator;
  @Mock private SecurityUtil securityUtil;

  @InjectMocks private CourseAssignmentService service;

  private UUID id;
  private UUID courseId;
  private UUID groupId;
  private UUID teacherId;
  private JCourse jCourse;
  private JGroup jGroup;
  private JTeacher jTeacher;
  private JCourseAssignment entity;
  private CourseAssignmentResponse response;
  private CourseAssignmentRequest request;

  @BeforeEach
  void setUp() {
    id = UUID.randomUUID();
    courseId = UUID.randomUUID();
    groupId = UUID.randomUUID();
    teacherId = UUID.randomUUID();
    jCourse =
        JCourse.builder()
            .id(courseId)
            .code("ALG1")
            .name("Algorithms")
            .credits(6)
            .studentLevel(StudentLevel.L1)
            .track(null)
            .build();
    jGroup = JGroup.builder().id(groupId).ref("G1").track(null).build();
    jTeacher = JTeacher.builder().id(teacherId).firstname("Ada").lastname("Lovelace").build();
    entity =
        JCourseAssignment.builder()
            .id(id)
            .course(jCourse)
            .group(jGroup)
            .teachers(new ArrayList<>(List.of(jTeacher)))
            .academicYear(2025)
            .semester(Semester.S1)
            .credits(6)
            .build();
    response =
        CourseAssignmentResponse.builder()
            .id(id)
            .courseId(courseId)
            .courseCode("ALG1")
            .courseName("Algorithms")
            .groupId(groupId)
            .groupRef("G1")
            .academicYear(2025)
            .semester(Semester.S1)
            .credits(6)
            .build();
    request =
        CourseAssignmentRequest.builder()
            .id(null)
            .courseId(courseId)
            .groupId(groupId)
            .teacherIds(List.of(teacherId))
            .academicYear(2025)
            .semester(Semester.S1)
            .credits(6)
            .build();
  }

  @Test
  void getByFilter_does_not_override_filters_for_admin() {
    var page = new PageImpl<>(List.of(entity));
    given(repository.findFilterPaged(null, null, null, null, Pageable.unpaged())).willReturn(page);
    given(mapper.toResponse(entity)).willReturn(response);

    var result = service.getByFilter(null, null, null, null, Pageable.unpaged());

    assertEquals(response, result.getContent().get(0));
  }

  @Test
  void getByFilter_forces_teacher_id_for_teacher() {
    given(securityUtil.isTeacher()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(teacherId);
    var page = new PageImpl<>(List.of(entity));
    given(repository.findFilterPaged(null, teacherId, null, null, Pageable.unpaged()))
        .willReturn(page);
    given(mapper.toResponse(entity)).willReturn(response);

    var result = service.getByFilter(null, null, null, null, Pageable.unpaged());

    assertEquals(response, result.getContent().get(0));
  }

  @Test
  void getByFilter_filters_by_current_group_for_student() {
    given(securityUtil.isStudent()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(UUID.randomUUID());
    given(groupFlowService.findCurrentGroup(any(UUID.class))).willReturn(Optional.of(jGroup));
    var page = new PageImpl<>(List.of(entity));
    given(repository.findFilterPaged(groupId, null, null, null, Pageable.unpaged()))
        .willReturn(page);
    given(mapper.toResponse(entity)).willReturn(response);

    var result = service.getByFilter(null, null, null, null, Pageable.unpaged());

    assertEquals(response, result.getContent().get(0));
  }

  @Test
  void getByFilter_returns_empty_page_for_student_without_group() {
    given(securityUtil.isStudent()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(UUID.randomUUID());
    given(groupFlowService.findCurrentGroup(any(UUID.class))).willReturn(Optional.empty());

    var result = service.getByFilter(null, null, null, null, Pageable.unpaged());

    assertTrue(result.isEmpty());
    then(repository).should(never()).findFilterPaged(any(), any(), any(), anyInt(), any());
  }

  @Test
  void getById_returns_response_when_found_and_not_student_or_teacher() {
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(mapper.toResponse(entity)).willReturn(response);

    var result = service.getById(id);

    assertEquals(response, result);
  }

  @Test
  void getById_throws_not_found_when_missing() {
    given(repository.findById(id)).willReturn(Optional.empty());

    var ex = assertThrows(ResourceNotFoundException.class, () -> service.getById(id));

    assertEquals("CourseAssignment with id:" + id + " not found.", ex.getMessage());
  }

  @Test
  void getById_allows_teacher_who_teaches_the_assignment() {
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(securityUtil.isTeacher()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(teacherId);
    given(mapper.toResponse(entity)).willReturn(response);

    var result = service.getById(id);

    assertEquals(response, result);
  }

  @Test
  void getById_throws_forbidden_for_teacher_not_teaching_the_assignment() {
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(securityUtil.isTeacher()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(UUID.randomUUID());

    var ex = assertThrows(ForbiddenAccessException.class, () -> service.getById(id));

    assertEquals("You may only access your own course assignments", ex.getMessage());
  }

  @Test
  void getById_allows_student_when_assignment_matches_current_or_past_group() {
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(securityUtil.isStudent()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(UUID.randomUUID());
    given(groupFlowService.historicJoinGroupIds(any(UUID.class))).willReturn(List.of(groupId));
    given(mapper.toResponse(entity)).willReturn(response);

    var result = service.getById(id);

    assertEquals(response, result);
  }

  @Test
  void getById_throws_forbidden_for_student_in_another_group() {
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(securityUtil.isStudent()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(UUID.randomUUID());
    given(groupFlowService.historicJoinGroupIds(any(UUID.class)))
        .willReturn(List.of(UUID.randomUUID()));

    var ex = assertThrows(ForbiddenAccessException.class, () -> service.getById(id));

    assertEquals("This course assignment is not part of your curriculum", ex.getMessage());
  }

  @Test
  void upsert_validates_credit_targets_and_saves_each_request() {
    given(courseService.getEntityOrThrow(courseId)).willReturn(jCourse);
    given(groupService.getEntityOrThrow(groupId)).willReturn(jGroup);
    given(courseMapper.toModel(jCourse)).willReturn(org.cocojojo.mg.model.Course.builder().build());
    given(groupMapper.toModel(jGroup)).willReturn(org.cocojojo.mg.model.Group.builder().build());
    given(teacherService.getEntityOrThrow(teacherId)).willReturn(jTeacher);
    given(mapper.toEntity(null, jCourse, jGroup, List.of(jTeacher), 2025, Semester.S1, 6))
        .willReturn(entity);
    given(repository.save(entity)).willReturn(entity);
    given(mapper.toResponse(entity)).willReturn(response);

    var result = service.upsert(List.of(request));

    assertEquals(1, result.size());
    assertEquals(response, result.get(0));
    then(validator).should().validateCreditTargets(List.of(request));
    then(validator).should().validateAllAreTeachers(List.of(teacherId));
    then(validator).should().validateNotDuplicate(null, courseId, groupId, 2025, Semester.S1);
  }

  @Test
  void upsert_updates_existing_assignment_when_id_present() {
    var updateRequest =
        CourseAssignmentRequest.builder()
            .id(id)
            .courseId(courseId)
            .groupId(groupId)
            .teacherIds(List.of(teacherId))
            .academicYear(2026)
            .semester(Semester.S2)
            .credits(10)
            .build();
    given(courseService.getEntityOrThrow(courseId)).willReturn(jCourse);
    given(groupService.getEntityOrThrow(groupId)).willReturn(jGroup);
    given(courseMapper.toModel(jCourse)).willReturn(org.cocojojo.mg.model.Course.builder().build());
    given(groupMapper.toModel(jGroup)).willReturn(org.cocojojo.mg.model.Group.builder().build());
    given(teacherService.getEntityOrThrow(teacherId)).willReturn(jTeacher);
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(repository.save(entity)).willReturn(entity);
    given(mapper.toResponse(entity)).willReturn(response);

    var result = service.upsert(List.of(updateRequest));

    assertEquals(1, result.size());
    assertEquals(2026, entity.getAcademicYear());
    assertEquals(Semester.S2, entity.getSemester());
    assertEquals(10, entity.getCredits());
    then(validator).should(never()).validateNotDuplicate(any(), any(), any(), anyInt(), any());
  }

  @Test
  void upsert_throws_not_found_when_updating_missing_assignment() {
    var updateRequest =
        CourseAssignmentRequest.builder()
            .id(id)
            .courseId(courseId)
            .groupId(groupId)
            .teacherIds(List.of(teacherId))
            .academicYear(2025)
            .semester(Semester.S1)
            .credits(6)
            .build();
    given(courseService.getEntityOrThrow(courseId)).willReturn(jCourse);
    given(groupService.getEntityOrThrow(groupId)).willReturn(jGroup);
    given(courseMapper.toModel(jCourse)).willReturn(org.cocojojo.mg.model.Course.builder().build());
    given(groupMapper.toModel(jGroup)).willReturn(org.cocojojo.mg.model.Group.builder().build());
    given(teacherService.getEntityOrThrow(teacherId)).willReturn(jTeacher);
    given(repository.findById(id)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.upsert(List.of(updateRequest)));
  }

  @Test
  void upsert_propagates_non_teacher_rejection() {
    doThrow(new IllegalArgumentException("User is not a teacher"))
        .when(validator)
        .validateAllAreTeachers(List.of(teacherId));

    assertThrows(IllegalArgumentException.class, () -> service.upsert(List.of(request)));
    then(repository).should(never()).save(any());
  }

  @Test
  void delete_removes_assignment_for_admin() {
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(examRepository.findByCourseAssignmentId(id)).willReturn(List.of());
    given(securityUtil.isAdmin()).willReturn(true);

    service.delete(id);

    then(repository).should().delete(entity);
  }

  @Test
  void delete_throws_conflict_when_exams_exist() {
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(examRepository.findByCourseAssignmentId(id)).willReturn(List.of(new JExam()));
    given(securityUtil.isAdmin()).willReturn(true);

    assertThrows(ConflictException.class, () -> service.delete(id));
    then(repository).should(never()).delete(any());
  }

  @Test
  void delete_throws_not_found_when_missing() {
    given(repository.findById(id)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.delete(id));
  }

  @Test
  void delete_throws_forbidden_for_non_admin() {
    given(repository.findById(id)).willReturn(Optional.of(entity));
    given(securityUtil.isAdmin()).willReturn(false);

    var ex = assertThrows(ForbiddenAccessException.class, () -> service.delete(id));

    assertEquals("Only an admin can remove a course assignment", ex.getMessage());
    then(repository).should(never()).delete(any());
  }

  @Test
  void curriculumStatus_marks_complete_when_credits_match_and_no_missing() {
    var groupModel = Group.builder().id(groupId).ref("G1").track(null).build();
    given(groupService.getById(groupId)).willReturn(groupModel);
    given(repository.findByGroupIdAndAcademicYearAndSemester(groupId, 2025, Semester.S1))
        .willReturn(List.of(entity));
    given(validator.targetCreditsPerSemester()).willReturn(6);
    var courseModel =
        Course.builder()
            .id(courseId)
            .code("ALG1")
            .name("Algorithms")
            .studentLevel(StudentLevel.L1)
            .track(null)
            .build();
    given(courseService.getByStudentLevelOrderByCodeAsc(StudentLevel.L1))
        .willReturn(List.of(courseModel));
    given(mapper.toResponse(entity)).willReturn(response);

    CurriculumStatusResponse result = service.curriculumStatus(groupId, 2025, Semester.S1);

    assertEquals(6, result.assignedCredits());
    assertEquals(6, result.targetCredits());
    assertEquals(true, result.complete());
    assertEquals(0, result.missingCourses().size());
  }

  @Test
  void curriculumStatus_lists_missing_courses_and_incomplete_when_short() {
    var groupModel = Group.builder().id(groupId).ref("G1").track(null).build();
    given(groupService.getById(groupId)).willReturn(groupModel);
    given(repository.findByGroupIdAndAcademicYearAndSemester(groupId, 2025, Semester.S1))
        .willReturn(List.of());
    var assignedCourse =
        Course.builder()
            .id(courseId)
            .code("ALG1")
            .name("Algorithms")
            .studentLevel(StudentLevel.L1)
            .track(null)
            .build();
    given(courseService.getByStudentLevelOrderByCodeAsc(StudentLevel.L1))
        .willReturn(List.of(assignedCourse));
    given(validator.targetCreditsPerSemester()).willReturn(30);

    CurriculumStatusResponse result = service.curriculumStatus(groupId, 2025, Semester.S1);

    assertEquals(0, result.assignedCredits());
    assertEquals(30, result.targetCredits());
    assertEquals(false, result.complete());
    assertEquals(1, result.missingCourses().size());
  }

  @Test
  void curriculumStatus_ignores_courses_of_another_track() {
    var groupModel = Group.builder().id(groupId).ref("G1").track(Track.EL).build();
    given(groupService.getById(groupId)).willReturn(groupModel);
    given(repository.findByGroupIdAndAcademicYearAndSemester(groupId, 2025, Semester.S1))
        .willReturn(List.of());
    var tnCourse =
        Course.builder()
            .id(courseId)
            .code("TN1")
            .name("Networking")
            .studentLevel(StudentLevel.L1)
            .track(Track.TN)
            .build();
    given(courseService.getByStudentLevelOrderByCodeAsc(StudentLevel.L1))
        .willReturn(List.of(tnCourse));

    CurriculumStatusResponse result = service.curriculumStatus(groupId, 2025, Semester.S1);

    assertEquals(0, result.missingCourses().size());
  }

  @Test
  void curriculumStatus_includes_trackless_and_same_track_courses() {
    var groupModel = Group.builder().id(groupId).ref("G1").track(Track.EL).build();
    given(groupService.getById(groupId)).willReturn(groupModel);
    given(repository.findByGroupIdAndAcademicYearAndSemester(groupId, 2025, Semester.S1))
        .willReturn(List.of());
    var common =
        Course.builder()
            .id(UUID.randomUUID())
            .code("COM")
            .name("Common")
            .studentLevel(StudentLevel.L1)
            .track(null)
            .build();
    var elCourse =
        Course.builder()
            .id(courseId)
            .code("EL1")
            .name("Electronics")
            .studentLevel(StudentLevel.L1)
            .track(Track.EL)
            .build();
    given(courseService.getByStudentLevelOrderByCodeAsc(StudentLevel.L1))
        .willReturn(List.of(common, elCourse));

    CurriculumStatusResponse result = service.curriculumStatus(groupId, 2025, Semester.S1);

    assertEquals(2, result.missingCourses().size());
  }
}
