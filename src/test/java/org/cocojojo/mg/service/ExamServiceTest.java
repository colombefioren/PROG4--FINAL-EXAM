package org.cocojojo.mg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.ExamRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.ExamResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.CourseAssignmentMapper;
import org.cocojojo.mg.mapper.ExamMapper;
import org.cocojojo.mg.model.CourseAssignment;
import org.cocojojo.mg.model.Fraction;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.ExamRepository;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JExam;
import org.cocojojo.mg.util.SecurityUtil;
import org.cocojojo.mg.validator.ExamValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

  @Mock private ExamRepository examRepository;
  @Mock private CourseAssignmentRepository courseAssignmentRepository;
  @Mock private ExamMapper mapper;
  @Mock private CourseAssignmentMapper courseAssignmentMapper;
  @Mock private ExamValidator validator;
  @Mock private SecurityUtil securityUtil;

  @InjectMocks private ExamService service;

  private UUID courseAssignmentId;
  private UUID examId;
  private JCourseAssignment assignmentEntity;
  private CourseAssignment assignmentModel;
  private JExam examEntity;
  private ExamResponse response;
  private ExamRequest request;

  @BeforeEach
  void setUp() {
    courseAssignmentId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    examId = UUID.fromString("77777777-7777-7777-7777-777777777777");
    assignmentEntity =
        JCourseAssignment.builder()
            .id(courseAssignmentId)
            .academicYear(2025)
            .semester(org.cocojojo.mg.model.enums.Semester.S1)
            .credits(6)
            .build();
    assignmentModel =
        CourseAssignment.builder()
            .id(courseAssignmentId)
            .academicYear(2025)
            .semester(org.cocojojo.mg.model.enums.Semester.S1)
            .credits(6)
            .build();
    examEntity =
        JExam.builder()
            .id(examId)
            .courseAssignment(assignmentEntity)
            .title("Midterm")
            .examDatetime(Instant.parse("2025-10-01T08:00:00Z"))
            .build();
    examEntity.setCoefficientFraction(new Fraction(1, 2));
    response =
        ExamResponse.builder()
            .id(examId)
            .courseAssignmentId(courseAssignmentId)
            .title("Midterm")
            .examDatetime(Instant.parse("2025-10-01T08:00:00Z"))
            .coefficient(new Fraction(1, 2))
            .build();
    request =
        ExamRequest.builder()
            .id(null)
            .title("Midterm")
            .examDatetime(Instant.parse("2025-10-01T08:00:00Z"))
            .coefficient(new Fraction(1, 2))
            .build();
  }

  @Test
  void getByCourseAssignmentId_returns_exams_for_admin() {
    given(courseAssignmentRepository.findById(courseAssignmentId))
        .willReturn(Optional.of(assignmentEntity));
    given(courseAssignmentMapper.toModel(assignmentEntity)).willReturn(assignmentModel);
    given(securityUtil.isAdmin()).willReturn(true);
    given(
            examRepository.findByCourseAssignmentIdAndDateRange(
                courseAssignmentId,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-12-31T00:00:00Z")))
        .willReturn(List.of(examEntity));
    given(mapper.toResponse(examEntity)).willReturn(response);

    var result =
        service.getByCourseAssignmentId(
            courseAssignmentId,
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2025-12-31T00:00:00Z"));

    assertEquals(List.of(response), result);
  }

  @Test
  void getByCourseAssignmentId_throws_not_found_when_assignment_missing() {
    given(courseAssignmentRepository.findById(courseAssignmentId)).willReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> service.getByCourseAssignmentId(courseAssignmentId, null, null));
    then(examRepository).should(never()).findByCourseAssignmentIdAndDateRange(any(), any(), any());
  }

  @Test
  void getByCourseAssignmentId_validates_teacher_teaches() {
    given(courseAssignmentRepository.findById(courseAssignmentId))
        .willReturn(Optional.of(assignmentEntity));
    given(courseAssignmentMapper.toModel(assignmentEntity)).willReturn(assignmentModel);
    given(securityUtil.isTeacher()).willReturn(true);
    var teacherId = UUID.fromString("66666666-6666-6666-6666-666666666666");
    given(securityUtil.getCurrentUserId()).willReturn(teacherId);
    doThrow(new ForbiddenAccessException("not your course"))
        .when(validator)
        .validateTeacherTeaches(teacherId, assignmentModel);

    assertThrows(
        ForbiddenAccessException.class,
        () -> service.getByCourseAssignmentId(courseAssignmentId, null, null));
  }

  @Test
  void getByCourseAssignmentId_validates_student_curriculum() {
    given(courseAssignmentRepository.findById(courseAssignmentId))
        .willReturn(Optional.of(assignmentEntity));
    given(courseAssignmentMapper.toModel(assignmentEntity)).willReturn(assignmentModel);
    given(securityUtil.isStudent()).willReturn(true);
    var studentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    given(securityUtil.getCurrentUserId()).willReturn(studentId);
    doThrow(new ForbiddenAccessException("not in curriculum"))
        .when(validator)
        .validateStudentInCurriculum(studentId, assignmentModel);

    assertThrows(
        ForbiddenAccessException.class,
        () -> service.getByCourseAssignmentId(courseAssignmentId, null, null));
  }

  @Test
  void getByCourseAssignmentId_throws_forbidden_for_unknown_role() {
    given(courseAssignmentRepository.findById(courseAssignmentId))
        .willReturn(Optional.of(assignmentEntity));
    given(courseAssignmentMapper.toModel(assignmentEntity)).willReturn(assignmentModel);

    var ex =
        assertThrows(
            ForbiddenAccessException.class,
            () -> service.getByCourseAssignmentId(courseAssignmentId, null, null));

    assertEquals("Only students, teachers and admins can view exams", ex.getMessage());
  }

  @Test
  void getById_returns_exam_when_assignment_matches() {
    given(examRepository.findById(examId)).willReturn(Optional.of(examEntity));
    given(securityUtil.isAdmin()).willReturn(true);
    given(courseAssignmentMapper.toModel(assignmentEntity)).willReturn(assignmentModel);
    given(mapper.toResponse(examEntity)).willReturn(response);

    var result = service.getById(courseAssignmentId, examId);

    assertEquals(response, result);
  }

  @Test
  void getById_throws_not_found_when_exam_missing() {
    given(examRepository.findById(examId)).willReturn(Optional.empty());

    var ex =
        assertThrows(
            ResourceNotFoundException.class, () -> service.getById(courseAssignmentId, examId));

    assertEquals("Exam with id:" + examId + " not found.", ex.getMessage());
  }

  @Test
  void getById_throws_not_found_when_exam_belongs_to_another_assignment() {
    var otherAssignment =
        JCourseAssignment.builder()
            .id(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
            .build();
    var otherExam =
        JExam.builder().id(examId).courseAssignment(otherAssignment).title("Other").build();
    given(examRepository.findById(examId)).willReturn(Optional.of(otherExam));

    assertThrows(
        ResourceNotFoundException.class, () -> service.getById(courseAssignmentId, examId));
  }

  @Test
  void upsert_creates_new_exam_for_admin() {
    given(courseAssignmentRepository.findById(courseAssignmentId))
        .willReturn(Optional.of(assignmentEntity));
    given(courseAssignmentMapper.toModel(assignmentEntity)).willReturn(assignmentModel);
    given(securityUtil.isAdmin()).willReturn(true);
    given(
            mapper.toEntity(
                null, assignmentEntity, "Midterm", request.examDatetime(), new Fraction(1, 2)))
        .willReturn(examEntity);
    given(examRepository.save(examEntity)).willReturn(examEntity);
    given(mapper.toResponse(examEntity)).willReturn(response);

    var result = service.upsert(courseAssignmentId, request);

    assertEquals(response, result);
  }

  @Test
  void upsert_updates_existing_exam_when_id_present() {
    var updateRequest =
        ExamRequest.builder()
            .id(examId)
            .title("Final")
            .examDatetime(Instant.parse("2025-12-01T08:00:00Z"))
            .coefficient(new Fraction(1, 1))
            .build();
    given(courseAssignmentRepository.findById(courseAssignmentId))
        .willReturn(Optional.of(assignmentEntity));
    given(courseAssignmentMapper.toModel(assignmentEntity)).willReturn(assignmentModel);
    given(securityUtil.isAdmin()).willReturn(true);
    given(examRepository.findById(examId)).willReturn(Optional.of(examEntity));
    given(examRepository.save(examEntity)).willReturn(examEntity);
    given(mapper.toResponse(examEntity)).willReturn(response);

    service.upsert(courseAssignmentId, updateRequest);

    assertEquals("Final", examEntity.getTitle());
    assertEquals(Instant.parse("2025-12-01T08:00:00Z"), examEntity.getExamDatetime());
    assertEquals(new Fraction(1, 1), examEntity.getCoefficientFraction());
  }

  @Test
  void upsert_throws_not_found_when_assignment_missing() {
    given(courseAssignmentRepository.findById(courseAssignmentId)).willReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> service.upsert(courseAssignmentId, request));
    then(examRepository).should(never()).save(any());
  }

  @Test
  void upsert_throws_forbidden_for_student() {
    given(courseAssignmentRepository.findById(courseAssignmentId))
        .willReturn(Optional.of(assignmentEntity));
    given(courseAssignmentMapper.toModel(assignmentEntity)).willReturn(assignmentModel);

    var ex =
        assertThrows(
            ForbiddenAccessException.class, () -> service.upsert(courseAssignmentId, request));

    assertEquals("Only admins and teachers can manage exams", ex.getMessage());
  }

  @Test
  void upsert_validates_teacher_teaches_for_teacher() {
    given(courseAssignmentRepository.findById(courseAssignmentId))
        .willReturn(Optional.of(assignmentEntity));
    given(courseAssignmentMapper.toModel(assignmentEntity)).willReturn(assignmentModel);
    given(securityUtil.isTeacher()).willReturn(true);
    var teacherId = UUID.fromString("66666666-6666-6666-6666-666666666666");
    given(securityUtil.getCurrentUserId()).willReturn(teacherId);
    doThrow(new ForbiddenAccessException("not your course"))
        .when(validator)
        .validateTeacherTeaches(teacherId, assignmentModel);

    assertThrows(ForbiddenAccessException.class, () -> service.upsert(courseAssignmentId, request));
  }

  @Test
  void delete_removes_exam_for_admin() {
    given(examRepository.findById(examId)).willReturn(Optional.of(examEntity));
    given(courseAssignmentMapper.toModel(assignmentEntity)).willReturn(assignmentModel);
    given(securityUtil.isAdmin()).willReturn(true);

    service.delete(courseAssignmentId, examId);

    then(examRepository).should().delete(examEntity);
  }

  @Test
  void delete_throws_not_found_when_exam_missing() {
    given(examRepository.findById(examId)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.delete(courseAssignmentId, examId));
  }

  @Test
  void delete_throws_not_found_when_exam_belongs_to_another_assignment() {
    var otherAssignment =
        JCourseAssignment.builder()
            .id(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
            .build();
    var otherExam =
        JExam.builder().id(examId).courseAssignment(otherAssignment).title("Other").build();
    given(examRepository.findById(examId)).willReturn(Optional.of(otherExam));

    assertThrows(ResourceNotFoundException.class, () -> service.delete(courseAssignmentId, examId));
    then(examRepository).should(never()).delete(any());
  }

  @Test
  void delete_throws_forbidden_for_student() {
    given(examRepository.findById(examId)).willReturn(Optional.of(examEntity));
    given(courseAssignmentMapper.toModel(assignmentEntity)).willReturn(assignmentModel);

    var ex =
        assertThrows(
            ForbiddenAccessException.class, () -> service.delete(courseAssignmentId, examId));

    assertEquals("Only admins and teachers can manage exams", ex.getMessage());
    then(examRepository).should(never()).delete(any());
  }
}
