package org.cocojojo.mg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import org.cocojojo.mg.model.Fraction;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.ExamRepository;
import org.cocojojo.mg.repository.GradeHistoryRepository;
import org.cocojojo.mg.repository.GradeRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.UserRepository;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JExam;
import org.cocojojo.mg.repository.model.JGrade;
import org.cocojojo.mg.repository.model.JGradeHistory;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.repository.model.JTeacher;
import org.cocojojo.mg.repository.model.JUser;
import org.cocojojo.mg.util.SecurityUtil;
import org.cocojojo.mg.validator.GradeValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

  @Mock private GradeRepository gradeRepository;
  @Mock private GradeHistoryRepository gradeHistoryRepository;
  @Mock private ExamRepository examRepository;
  @Mock private StudentRepository studentRepository;
  @Mock private UserRepository userRepository;
  @Mock private GradeMapper mapper;
  @Mock private GradeHistoryMapper historyMapper;
  @Mock private CourseAssignmentMapper courseAssignmentMapper;
  @Mock private GradeValidator validator;
  @Mock private SecurityUtil securityUtil;

  @InjectMocks private GradeService service;

  private UUID examId;
  private UUID studentId;
  private UUID gradeId;
  private UUID teacherId;
  private JTeacher teacher;
  private JStudent student;
  private JUser changedBy;
  private JCourse jCourse;
  private JGroup jGroup;
  private JCourseAssignment assignment;
  private JExam exam;
  private JGrade grade;
  private JGradeHistory history;
  private GradeResponse response;
  private GradeHistoryResponse historyResponse;
  private CourseAssignment assignmentModel;

  @BeforeEach
  void setUp() {
    examId = UUID.randomUUID();
    studentId = UUID.randomUUID();
    gradeId = UUID.randomUUID();
    teacherId = UUID.randomUUID();
    teacher = JTeacher.builder().id(teacherId).firstname("Ada").lastname("Lovelace").build();
    student =
        JStudent.builder()
            .id(studentId)
            .firstname("Grace")
            .lastname("Hopper")
            .std("STD25001")
            .build();
    changedBy = JUser.builder().id(teacherId).firstname("Ada").lastname("Lovelace").build();
    jCourse =
        JCourse.builder()
            .id(UUID.randomUUID())
            .code("ALG1")
            .name("Algorithms")
            .studentLevel(StudentLevel.L1)
            .track(Track.EL)
            .build();
    jGroup = JGroup.builder().id(UUID.randomUUID()).ref("G1").build();
    assignment =
        JCourseAssignment.builder()
            .id(UUID.randomUUID())
            .course(jCourse)
            .group(jGroup)
            .teachers(List.of(teacher))
            .academicYear(2025)
            .semester(Semester.S1)
            .credits(6)
            .build();
    assignmentModel =
        CourseAssignment.builder()
            .id(assignment.getId())
            .teachers(List.of(org.cocojojo.mg.model.Teacher.builder().id(teacherId).build()))
            .build();
    exam =
        JExam.builder()
            .id(examId)
            .courseAssignment(assignment)
            .title("Midterm")
            .examDatetime(java.time.Instant.parse("2025-10-01T08:00:00Z"))
            .build();
    exam.setCoefficientFraction(new Fraction(1, 2));
    grade =
        JGrade.builder()
            .id(gradeId)
            .student(student)
            .exam(exam)
            .value(new BigDecimal("15.50"))
            .comment("good")
            .build();
    response =
        GradeResponse.builder()
            .id(gradeId)
            .studentId(studentId)
            .studentStd("STD25001")
            .examId(examId)
            .examTitle("Midterm")
            .courseCode("ALG1")
            .value(new BigDecimal("15.50"))
            .comment("good")
            .build();
    history =
        JGradeHistory.builder()
            .id(UUID.randomUUID())
            .grade(grade)
            .previousValue(new BigDecimal("12.00"))
            .newValue(new BigDecimal("15.50"))
            .reason("recheck")
            .changedBy(changedBy)
            .build();
    historyResponse =
        GradeHistoryResponse.builder()
            .id(history.getId())
            .gradeId(gradeId)
            .previousValue(new BigDecimal("12.00"))
            .newValue(new BigDecimal("15.50"))
            .reason("recheck")
            .changedById(teacherId)
            .changedByName("Ada Lovelace")
            .build();
  }

  @Test
  void getByExamId_returns_grades_for_admin() {
    given(examRepository.findWithDetailsById(examId)).willReturn(Optional.of(exam));
    given(courseAssignmentMapper.toModel(assignment)).willReturn(assignmentModel);
    given(securityUtil.isAdmin()).willReturn(true);
    given(gradeRepository.findByExamId(examId)).willReturn(List.of(grade));
    given(mapper.toResponse(grade)).willReturn(response);

    var result = service.getByExamId(examId);

    assertEquals(List.of(response), result);
  }

  @Test
  void getByExamId_throws_not_found_when_exam_missing() {
    given(examRepository.findWithDetailsById(examId)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.getByExamId(examId));
  }

  @Test
  void getByExamId_throws_forbidden_for_student() {
    given(examRepository.findWithDetailsById(examId)).willReturn(Optional.of(exam));
    given(courseAssignmentMapper.toModel(assignment)).willReturn(assignmentModel);

    var ex = assertThrows(ForbiddenAccessException.class, () -> service.getByExamId(examId));

    assertEquals("Only teachers and admins can manage grades", ex.getMessage());
  }

  @Test
  void getByExamId_validates_teacher_teaches() {
    given(examRepository.findWithDetailsById(examId)).willReturn(Optional.of(exam));
    given(courseAssignmentMapper.toModel(assignment)).willReturn(assignmentModel);
    given(securityUtil.isTeacher()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(teacherId);
    doThrow(new ForbiddenAccessException("not yours"))
        .when(validator)
        .validateTeacherTeaches(teacherId, assignmentModel);

    assertThrows(ForbiddenAccessException.class, () -> service.getByExamId(examId));
  }

  @Test
  void getByExamIdAndStudentId_returns_grade_when_found() {
    given(examRepository.findWithDetailsById(examId)).willReturn(Optional.of(exam));
    given(courseAssignmentMapper.toModel(assignment)).willReturn(assignmentModel);
    given(securityUtil.isAdmin()).willReturn(true);
    given(gradeRepository.findByExamIdAndStudentId(examId, studentId))
        .willReturn(Optional.of(grade));
    given(mapper.toResponse(grade)).willReturn(response);

    var result = service.getByExamIdAndStudentId(examId, studentId);

    assertEquals(response, result);
  }

  @Test
  void getByExamIdAndStudentId_throws_not_found_when_grade_missing() {
    given(examRepository.findWithDetailsById(examId)).willReturn(Optional.of(exam));
    given(courseAssignmentMapper.toModel(assignment)).willReturn(assignmentModel);
    given(securityUtil.isAdmin()).willReturn(true);
    given(gradeRepository.findByExamIdAndStudentId(examId, studentId)).willReturn(Optional.empty());

    var ex =
        assertThrows(
            ResourceNotFoundException.class,
            () -> service.getByExamIdAndStudentId(examId, studentId));

    assertEquals(
        "Grade not found for exam " + examId + " and student " + studentId, ex.getMessage());
  }

  @Test
  void create_saves_grades_and_records_history() {
    var gradeRequest =
        GradeRequest.builder()
            .studentId(studentId)
            .value(new BigDecimal("15.50"))
            .comment("good")
            .build();
    given(examRepository.findWithDetailsById(examId)).willReturn(Optional.of(exam));
    given(courseAssignmentMapper.toModel(assignment)).willReturn(assignmentModel);
    given(securityUtil.isAdmin()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(teacherId);
    given(userRepository.findById(teacherId)).willReturn(Optional.of(changedBy));
    given(studentRepository.findById(studentId)).willReturn(Optional.of(student));
    given(gradeRepository.findByExamIdAndStudentId(examId, studentId)).willReturn(Optional.empty());
    given(mapper.toEntity(null, student, exam, new BigDecimal("15.50"), "good")).willReturn(grade);
    given(gradeRepository.save(grade)).willReturn(grade);
    given(mapper.toResponse(grade)).willReturn(response);
    given(
            historyMapper.toEntity(
                null, grade, null, new BigDecimal("15.50"), "Grade recorded", changedBy))
        .willReturn(history);
    given(gradeHistoryRepository.save(history)).willReturn(history);

    var result = service.create(examId, List.of(gradeRequest));

    assertEquals(List.of(response), result);
    then(gradeHistoryRepository).should().save(history);
  }

  @Test
  void create_throws_when_student_missing() {
    var gradeRequest =
        GradeRequest.builder().studentId(studentId).value(new BigDecimal("15.50")).build();
    given(examRepository.findWithDetailsById(examId)).willReturn(Optional.of(exam));
    given(courseAssignmentMapper.toModel(assignment)).willReturn(assignmentModel);
    given(securityUtil.isAdmin()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(teacherId);
    given(userRepository.findById(teacherId)).willReturn(Optional.of(changedBy));
    given(studentRepository.findById(studentId)).willReturn(Optional.empty());

    var ex =
        assertThrows(
            ResourceNotFoundException.class, () -> service.create(examId, List.of(gradeRequest)));

    assertEquals("Student with id:" + studentId + " not found.", ex.getMessage());
  }

  @Test
  void create_rejects_duplicate_grade_for_same_exam_and_student() {
    var gradeRequest =
        GradeRequest.builder().studentId(studentId).value(new BigDecimal("15.50")).build();
    given(examRepository.findWithDetailsById(examId)).willReturn(Optional.of(exam));
    given(courseAssignmentMapper.toModel(assignment)).willReturn(assignmentModel);
    given(securityUtil.isAdmin()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(teacherId);
    given(userRepository.findById(teacherId)).willReturn(Optional.of(changedBy));
    given(studentRepository.findById(studentId)).willReturn(Optional.of(student));
    given(gradeRepository.findByExamIdAndStudentId(examId, studentId))
        .willReturn(Optional.of(grade));

    assertThrows(
        IllegalArgumentException.class, () -> service.create(examId, List.of(gradeRequest)));
    then(gradeRepository).should(never()).save(any());
  }

  @Test
  void create_throws_when_authenticated_user_missing() {
    var gradeRequest =
        GradeRequest.builder().studentId(studentId).value(new BigDecimal("15.50")).build();
    given(examRepository.findWithDetailsById(examId)).willReturn(Optional.of(exam));
    given(courseAssignmentMapper.toModel(assignment)).willReturn(assignmentModel);
    given(securityUtil.isAdmin()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(teacherId);
    given(userRepository.findById(teacherId)).willReturn(Optional.empty());

    assertThrows(IllegalStateException.class, () -> service.create(examId, List.of(gradeRequest)));
  }

  @Test
  void correct_updates_value_and_records_history() {
    var correction =
        GradeCorrectionRequest.builder().value(new BigDecimal("18.00")).reason("recheck").build();
    given(examRepository.findWithDetailsById(examId)).willReturn(Optional.of(exam));
    given(courseAssignmentMapper.toModel(assignment)).willReturn(assignmentModel);
    given(securityUtil.isAdmin()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(teacherId);
    given(userRepository.findById(teacherId)).willReturn(Optional.of(changedBy));
    given(gradeRepository.findByExamIdAndStudentId(examId, studentId))
        .willReturn(Optional.of(grade));
    given(
            historyMapper.toEntity(
                null,
                grade,
                new BigDecimal("15.50"),
                new BigDecimal("18.00"),
                "recheck",
                changedBy))
        .willReturn(history);
    given(gradeHistoryRepository.save(history)).willReturn(history);
    given(gradeRepository.save(grade)).willReturn(grade);
    given(gradeHistoryRepository.findByGradeIdOrderByChangedAtDesc(gradeId))
        .willReturn(List.of(history));
    given(historyMapper.toResponse(history)).willReturn(historyResponse);

    var result = service.correct(examId, studentId, correction);

    assertEquals(List.of(historyResponse), result);
    assertEquals(new BigDecimal("18.00"), grade.getValue());
  }

  @Test
  void correct_throws_not_found_when_grade_missing() {
    var correction =
        GradeCorrectionRequest.builder().value(new BigDecimal("18.00")).reason("recheck").build();
    given(examRepository.findWithDetailsById(examId)).willReturn(Optional.of(exam));
    given(courseAssignmentMapper.toModel(assignment)).willReturn(assignmentModel);
    given(securityUtil.isAdmin()).willReturn(true);
    given(gradeRepository.findByExamIdAndStudentId(examId, studentId)).willReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> service.correct(examId, studentId, correction));
  }

  @Test
  void getByStudentId_returns_all_grades_for_admin_or_student_self() {
    given(gradeRepository.findByStudentId(studentId)).willReturn(List.of(grade));
    given(mapper.toResponse(grade)).willReturn(response);

    var result = service.getByStudentId(studentId);

    assertEquals(List.of(response), result);
  }

  @Test
  void getByStudentId_filters_grades_for_teacher() {
    given(securityUtil.isTeacher()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(teacherId);
    given(gradeRepository.findByStudentId(studentId)).willReturn(List.of(grade));
    given(mapper.toResponse(grade)).willReturn(response);

    var result = service.getByStudentId(studentId);

    assertEquals(List.of(response), result);
  }

  @Test
  void getByStudentId_drops_grades_from_courses_teacher_does_not_teach() {
    var otherTeacher = JTeacher.builder().id(UUID.randomUUID()).build();
    var otherAssignment =
        JCourseAssignment.builder().id(UUID.randomUUID()).teachers(List.of(otherTeacher)).build();
    var otherExam =
        JExam.builder()
            .id(UUID.randomUUID())
            .courseAssignment(otherAssignment)
            .title("Other")
            .build();
    var otherGrade =
        JGrade.builder()
            .id(gradeId)
            .student(student)
            .exam(otherExam)
            .value(new BigDecimal("10.00"))
            .build();
    given(securityUtil.isTeacher()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(teacherId);
    given(gradeRepository.findByStudentId(studentId)).willReturn(List.of(grade, otherGrade));
    given(mapper.toResponse(grade)).willReturn(response);

    var result = service.getByStudentId(studentId);

    assertEquals(List.of(response), result);
  }

  @Test
  void getById_returns_for_admin() {
    given(gradeRepository.findWithDetailsById(gradeId)).willReturn(Optional.of(grade));
    given(securityUtil.isAdmin()).willReturn(true);
    given(mapper.toResponse(grade)).willReturn(response);

    assertEquals(response, service.getById(gradeId));
  }

  @Test
  void getById_validates_teacher_teaches() {
    given(gradeRepository.findWithDetailsById(gradeId)).willReturn(Optional.of(grade));
    given(securityUtil.isTeacher()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(teacherId);
    given(courseAssignmentMapper.toModel(assignment)).willReturn(assignmentModel);
    doThrow(new ForbiddenAccessException("not yours"))
        .when(validator)
        .validateTeacherTeaches(teacherId, assignmentModel);

    assertThrows(ForbiddenAccessException.class, () -> service.getById(gradeId));
  }

  @Test
  void getById_requires_self_for_student() {
    given(gradeRepository.findWithDetailsById(gradeId)).willReturn(Optional.of(grade));
    given(securityUtil.isStudent()).willReturn(true);
    doThrow(new ForbiddenAccessException("not yours"))
        .when(securityUtil)
        .requireSelfOrAdmin(studentId);

    assertThrows(ForbiddenAccessException.class, () -> service.getById(gradeId));
  }

  @Test
  void getById_returns_for_student_self() {
    given(gradeRepository.findWithDetailsById(gradeId)).willReturn(Optional.of(grade));
    given(securityUtil.isStudent()).willReturn(true);
    given(mapper.toResponse(grade)).willReturn(response);

    assertEquals(response, service.getById(gradeId));
  }

  @Test
  void getById_throws_for_unknown_role() {
    given(gradeRepository.findWithDetailsById(gradeId)).willReturn(Optional.of(grade));

    var ex = assertThrows(ForbiddenAccessException.class, () -> service.getById(gradeId));

    assertEquals("Only students, teachers and admins can view grades", ex.getMessage());
  }

  @Test
  void getById_throws_not_found_when_grade_missing() {
    given(gradeRepository.findWithDetailsById(gradeId)).willReturn(Optional.empty());

    var ex = assertThrows(ResourceNotFoundException.class, () -> service.getById(gradeId));

    assertEquals("Grade with id:" + gradeId + " not found.", ex.getMessage());
  }

  @Test
  void delete_records_history_and_deletes_grade() {
    given(gradeRepository.findWithDetailsById(gradeId)).willReturn(Optional.of(grade));
    given(courseAssignmentMapper.toModel(assignment)).willReturn(assignmentModel);
    given(securityUtil.isAdmin()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(teacherId);
    given(userRepository.findById(teacherId)).willReturn(Optional.of(changedBy));
    given(
            historyMapper.toEntity(
                null, grade, new BigDecimal("15.50"), null, "grade error", changedBy))
        .willReturn(history);
    given(gradeHistoryRepository.save(history)).willReturn(history);
    given(gradeHistoryRepository.findByGradeIdOrderByChangedAtDesc(gradeId))
        .willReturn(List.of(history));
    given(historyMapper.toResponse(history)).willReturn(historyResponse);

    var result = service.delete(gradeId, "grade error");

    then(gradeRepository).should().delete(grade);
    assertEquals(List.of(historyResponse), result);
  }

  @Test
  void delete_throws_forbidden_for_student() {
    given(gradeRepository.findWithDetailsById(gradeId)).willReturn(Optional.of(grade));
    given(courseAssignmentMapper.toModel(assignment)).willReturn(assignmentModel);

    var ex = assertThrows(ForbiddenAccessException.class, () -> service.delete(gradeId, "reason"));

    assertEquals("Only teachers and admins can manage grades", ex.getMessage());
    then(gradeRepository).should(never()).delete(any());
  }

  @Test
  void getHistory_returns_history_for_admin() {
    given(gradeRepository.findWithDetailsById(gradeId)).willReturn(Optional.of(grade));
    given(securityUtil.isAdmin()).willReturn(true);
    given(gradeHistoryRepository.findByGradeIdOrderByChangedAtDesc(gradeId))
        .willReturn(List.of(history));
    given(historyMapper.toResponse(history)).willReturn(historyResponse);

    var result = service.getHistory(gradeId);

    assertEquals(List.of(historyResponse), result);
  }

  @Test
  void getHistory_returns_history_for_teacher_who_teaches() {
    given(gradeRepository.findWithDetailsById(gradeId)).willReturn(Optional.of(grade));
    given(securityUtil.isTeacher()).willReturn(true);
    given(securityUtil.getCurrentUserId()).willReturn(teacherId);
    given(courseAssignmentMapper.toModel(assignment)).willReturn(assignmentModel);
    given(gradeHistoryRepository.findByGradeIdOrderByChangedAtDesc(gradeId))
        .willReturn(List.of(history));
    given(historyMapper.toResponse(history)).willReturn(historyResponse);

    var result = service.getHistory(gradeId);

    assertEquals(List.of(historyResponse), result);
  }

  @Test
  void getHistory_returns_history_for_student_self() {
    given(gradeRepository.findWithDetailsById(gradeId)).willReturn(Optional.of(grade));
    given(securityUtil.isStudent()).willReturn(true);
    given(gradeHistoryRepository.findByGradeIdOrderByChangedAtDesc(gradeId))
        .willReturn(List.of(history));
    given(historyMapper.toResponse(history)).willReturn(historyResponse);

    var result = service.getHistory(gradeId);

    assertEquals(List.of(historyResponse), result);
  }

  @Test
  void getHistory_throws_for_unknown_role() {
    given(gradeRepository.findWithDetailsById(gradeId)).willReturn(Optional.of(grade));

    var ex = assertThrows(ForbiddenAccessException.class, () -> service.getHistory(gradeId));

    assertEquals("Only students, teachers and admins can view grade history", ex.getMessage());
  }
}
