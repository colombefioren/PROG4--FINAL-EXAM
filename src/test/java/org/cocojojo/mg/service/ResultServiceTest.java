package org.cocojojo.mg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResultResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.ResultsSummaryResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.model.Fraction;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.model.enums.ResultStatus;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.ExamRepository;
import org.cocojojo.mg.repository.GradeRepository;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JExam;
import org.cocojojo.mg.repository.model.JGrade;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JGroupFlow;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.util.SemesterCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

  @Mock private StudentRepository studentRepository;
  @Mock private GroupFlowRepository groupFlowRepository;
  @Mock private CourseAssignmentRepository courseAssignmentRepository;
  @Mock private ExamRepository examRepository;
  @Mock private GradeRepository gradeRepository;

  @InjectMocks private ResultService service;

  private UUID studentId;
  private UUID groupId;
  private UUID courseId;
  private UUID assignmentId;
  private UUID examId;
  private JStudent student;
  private JPromotion promotion;
  private JGroup group;
  private JGroupFlow joinFlow;
  private JCourse course;
  private JCourseAssignment assignment;
  private JExam exam;
  private JGrade grade;

  @BeforeEach
  void setUp() {
    studentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    groupId = UUID.fromString("55555555-5555-5555-5555-555555555555");
    courseId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    assignmentId = UUID.fromString("99999999-9999-9999-9999-999999999999");
    examId = UUID.fromString("77777777-7777-7777-7777-777777777777");
    promotion =
        JPromotion.builder().id(UUID.randomUUID()).ref("2025").name("2025").entryYear(2024).build();
    student =
        JStudent.builder()
            .id(studentId)
            .firstname("Grace")
            .lastname("Hopper")
            .std("STD24001")
            .promotion(promotion)
            .build();
    group = JGroup.builder().id(groupId).ref("G1").track(Track.EL).build();
    joinFlow =
        JGroupFlow.builder()
            .id(UUID.randomUUID())
            .student(student)
            .group(group)
            .groupFlowType(GroupFlowType.JOIN)
            .build();
    joinFlow.setCreatedAt(Instant.parse("2025-06-01T00:00:00Z"));
    course =
        JCourse.builder()
            .id(courseId)
            .code("ALG1")
            .name("Algorithms")
            .credits(6)
            .studentLevel(StudentLevel.L1)
            .track(Track.EL)
            .build();
    assignment =
        JCourseAssignment.builder()
            .id(assignmentId)
            .course(course)
            .group(group)
            .academicYear(2024)
            .semester(Semester.S1)
            .credits(6)
            .build();
    exam =
        JExam.builder()
            .id(examId)
            .courseAssignment(assignment)
            .title("Midterm")
            .examDatetime(Instant.parse("2024-10-01T08:00:00Z"))
            .build();
    exam.setCoefficientFraction(new Fraction(1, 1));
    grade =
        JGrade.builder()
            .id(UUID.randomUUID())
            .student(student)
            .exam(exam)
            .value(new BigDecimal("14.00"))
            .build();
  }

  private void stubSingleStudentFlow() {
    given(groupFlowRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(List.of(joinFlow));
    given(groupFlowRepository.findFirstByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(Optional.of(joinFlow));
  }

  @Test
  void computeYearlyResult_throws_not_found_when_student_missing() {
    given(studentRepository.findById(studentId)).willReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> service.computeYearlyResult(studentId, StudentLevel.L1));
  }

  @Test
  void computeYearlyResult_is_completed_when_everything_graded() {
    given(studentRepository.findById(studentId)).willReturn(Optional.of(student));
    stubSingleStudentFlow();
    given(
            courseAssignmentRepository.findCurriculumCourses(
                List.of(groupId), StudentLevel.L1.semesters()))
        .willReturn(List.of(course));
    given(
            courseAssignmentRepository.findByCourseIdAndSemesterInAndGroupIdIn(
                courseId, StudentLevel.L1.semesters(), List.of(groupId)))
        .willReturn(List.of(assignment));
    given(
            gradeRepository.findByStudentAndCourseAndSemesters(
                studentId, courseId, StudentLevel.L1.semesters()))
        .willReturn(List.of(grade));
    given(
            examRepository.findByCourseAndSemestersAndGroups(
                courseId, StudentLevel.L1.semesters(), List.of(groupId)))
        .willReturn(List.of(exam));

    var result = service.computeYearlyResult(studentId, StudentLevel.L1);

    assertEquals(ResultStatus.COMPLETED, result.status());
    assertEquals(new BigDecimal("14.00"), result.overallAverage());
    assertEquals(6, result.earnedCredits());
    assertEquals(6, result.totalCredits());
    assertEquals(1, result.courses().size());
    assertEquals(Boolean.TRUE, result.courses().get(0).passed());
  }

  @Test
  void computeYearlyResult_is_provisional_when_a_grade_is_missing() {
    given(studentRepository.findById(studentId)).willReturn(Optional.of(student));
    stubSingleStudentFlow();
    given(
            courseAssignmentRepository.findCurriculumCourses(
                List.of(groupId), StudentLevel.L1.semesters()))
        .willReturn(List.of(course));
    given(
            courseAssignmentRepository.findByCourseIdAndSemesterInAndGroupIdIn(
                courseId, StudentLevel.L1.semesters(), List.of(groupId)))
        .willReturn(List.of(assignment));
    given(
            gradeRepository.findByStudentAndCourseAndSemesters(
                studentId, courseId, StudentLevel.L1.semesters()))
        .willReturn(List.of());
    given(
            examRepository.findByCourseAndSemestersAndGroups(
                courseId, StudentLevel.L1.semesters(), List.of(groupId)))
        .willReturn(List.of(exam));

    var result = service.computeYearlyResult(studentId, StudentLevel.L1);

    assertEquals(ResultStatus.PROVISIONAL, result.status());
    CourseResultResponse courseResult = result.courses().get(0);
    assertEquals(Boolean.FALSE, courseResult.graded());
    assertNull(courseResult.average());
    assertEquals(Boolean.FALSE, courseResult.complete());
    assertNull(courseResult.passed());
  }

  @Test
  void computeYearlyResult_is_provisional_when_coefficients_do_not_sum_to_one() {
    given(studentRepository.findById(studentId)).willReturn(Optional.of(student));
    stubSingleStudentFlow();
    given(
            courseAssignmentRepository.findCurriculumCourses(
                List.of(groupId), StudentLevel.L1.semesters()))
        .willReturn(List.of(course));
    given(
            courseAssignmentRepository.findByCourseIdAndSemesterInAndGroupIdIn(
                courseId, StudentLevel.L1.semesters(), List.of(groupId)))
        .willReturn(List.of(assignment));
    var halfExam =
        JExam.builder()
            .id(examId)
            .courseAssignment(assignment)
            .title("Half")
            .examDatetime(Instant.parse("2024-10-01T08:00:00Z"))
            .build();
    halfExam.setCoefficientFraction(new Fraction(1, 2));
    var halfGrade =
        JGrade.builder()
            .id(UUID.randomUUID())
            .student(student)
            .exam(halfExam)
            .value(new BigDecimal("14.00"))
            .build();
    given(
            gradeRepository.findByStudentAndCourseAndSemesters(
                studentId, courseId, StudentLevel.L1.semesters()))
        .willReturn(List.of(halfGrade));
    given(
            examRepository.findByCourseAndSemestersAndGroups(
                courseId, StudentLevel.L1.semesters(), List.of(groupId)))
        .willReturn(List.of(halfExam));

    var result = service.computeYearlyResult(studentId, StudentLevel.L1);

    assertEquals(ResultStatus.PROVISIONAL, result.status());
    assertEquals(Boolean.FALSE, result.courses().get(0).complete());
    assertEquals(Boolean.FALSE, result.courses().get(0).passed());
  }

  @Test
  void computeYearlyResult_marks_failed_when_average_below_threshold() {
    given(studentRepository.findById(studentId)).willReturn(Optional.of(student));
    stubSingleStudentFlow();
    given(
            courseAssignmentRepository.findCurriculumCourses(
                List.of(groupId), StudentLevel.L1.semesters()))
        .willReturn(List.of(course));
    given(
            courseAssignmentRepository.findByCourseIdAndSemesterInAndGroupIdIn(
                courseId, StudentLevel.L1.semesters(), List.of(groupId)))
        .willReturn(List.of(assignment));
    var failingGrade =
        JGrade.builder()
            .id(UUID.randomUUID())
            .student(student)
            .exam(exam)
            .value(new BigDecimal("8.00"))
            .build();
    given(
            gradeRepository.findByStudentAndCourseAndSemesters(
                studentId, courseId, StudentLevel.L1.semesters()))
        .willReturn(List.of(failingGrade));
    given(
            examRepository.findByCourseAndSemestersAndGroups(
                courseId, StudentLevel.L1.semesters(), List.of(groupId)))
        .willReturn(List.of(exam));

    var result = service.computeYearlyResult(studentId, StudentLevel.L1);

    assertEquals(ResultStatus.PROVISIONAL, result.status());
    assertEquals(Boolean.TRUE, result.courses().get(0).complete());
    assertEquals(Boolean.FALSE, result.courses().get(0).passed());
    assertEquals(0, result.earnedCredits());
    assertEquals(new BigDecimal("8.00"), result.courses().get(0).average());
  }

  @Test
  void computeYearlyResult_excludes_courses_of_another_track() {
    given(studentRepository.findById(studentId)).willReturn(Optional.of(student));
    stubSingleStudentFlow();
    var tnCourse =
        JCourse.builder()
            .id(UUID.randomUUID())
            .code("TN1")
            .name("Networking")
            .credits(6)
            .studentLevel(StudentLevel.L1)
            .track(Track.TN)
            .build();
    given(
            courseAssignmentRepository.findCurriculumCourses(
                List.of(groupId), StudentLevel.L1.semesters()))
        .willReturn(List.of(course, tnCourse));
    given(
            courseAssignmentRepository.findByCourseIdAndSemesterInAndGroupIdIn(
                courseId, StudentLevel.L1.semesters(), List.of(groupId)))
        .willReturn(List.of(assignment));
    given(
            gradeRepository.findByStudentAndCourseAndSemesters(
                studentId, courseId, StudentLevel.L1.semesters()))
        .willReturn(List.of(grade));
    given(
            examRepository.findByCourseAndSemestersAndGroups(
                courseId, StudentLevel.L1.semesters(), List.of(groupId)))
        .willReturn(List.of(exam));

    var result = service.computeYearlyResult(studentId, StudentLevel.L1);

    assertEquals(1, result.courses().size());
    assertEquals("ALG1", result.courses().get(0).courseCode());
  }

  @Test
  void computeResultsSummary_graduates_when_all_levels_completed() {
    given(studentRepository.findById(studentId)).willReturn(Optional.of(student));
    stubSingleStudentFlow();
    given(courseAssignmentRepository.findCurriculumCourses(anyCollection(), anyCollection()))
        .willReturn(List.of(course));
    given(
            courseAssignmentRepository.findByCourseIdAndSemesterInAndGroupIdIn(
                eq(courseId), anyCollection(), anyCollection()))
        .willReturn(List.of(assignment));
    given(
            gradeRepository.findByStudentAndCourseAndSemesters(
                eq(studentId), eq(courseId), anyCollection()))
        .willReturn(List.of(grade));
    given(
            examRepository.findByCourseAndSemestersAndGroups(
                eq(courseId), anyCollection(), anyCollection()))
        .willReturn(List.of(exam));

    var result = service.computeResultsSummary(studentId);

    assertTrue(result.graduate());
    assertEquals(3, result.levels().size());
  }

  @Test
  void computeResultsSummary_does_not_graduate_when_a_level_is_provisional() {
    given(studentRepository.findById(studentId)).willReturn(Optional.of(student));
    stubSingleStudentFlow();
    given(courseAssignmentRepository.findCurriculumCourses(anyCollection(), anyCollection()))
        .willReturn(List.of(course));
    given(
            courseAssignmentRepository.findByCourseIdAndSemesterInAndGroupIdIn(
                eq(courseId), anyCollection(), anyCollection()))
        .willReturn(List.of());

    var result = service.computeResultsSummary(studentId);

    assertFalse(result.graduate());
  }

  @Test
  void computeResultsSummary_throws_not_found_when_student_missing() {
    given(studentRepository.findById(studentId)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.computeResultsSummary(studentId));
  }

  @Test
  void currentTrack_returns_track_when_latest_flow_is_join_on_tracked_group() {
    given(studentRepository.findById(studentId)).willReturn(Optional.of(student));
    given(groupFlowRepository.findFirstByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(Optional.of(joinFlow));

    assertEquals(Track.EL, service.currentTrack(studentId));
  }

  @Test
  void currentTrack_returns_null_when_latest_flow_is_leave() {
    var leaveFlow =
        JGroupFlow.builder()
            .id(UUID.randomUUID())
            .student(student)
            .group(group)
            .groupFlowType(GroupFlowType.LEAVE)
            .build();
    leaveFlow.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
    given(studentRepository.findById(studentId)).willReturn(Optional.of(student));
    given(groupFlowRepository.findFirstByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(Optional.of(leaveFlow));

    assertNull(service.currentTrack(studentId));
  }

  @Test
  void currentTrack_returns_null_when_no_flow() {
    given(studentRepository.findById(studentId)).willReturn(Optional.of(student));
    given(groupFlowRepository.findFirstByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(Optional.empty());

    assertNull(service.currentTrack(studentId));
  }

  @Test
  void currentTrack_defaults_to_el_when_student_is_past_s4_without_track() {
    int entryYear = SemesterCalculator.entryYearFor(Semester.S4, LocalDate.now());
    var noTrackPromotion =
        JPromotion.builder().id(UUID.randomUUID()).ref("P").name("P").entryYear(entryYear).build();
    var noTrackGroup = JGroup.builder().id(groupId).ref("G1").track(null).build();
    var noTrackFlow =
        JGroupFlow.builder()
            .id(UUID.randomUUID())
            .student(student)
            .group(noTrackGroup)
            .groupFlowType(GroupFlowType.JOIN)
            .build();
    student.setPromotion(noTrackPromotion);
    given(studentRepository.findById(studentId)).willReturn(Optional.of(student));
    given(groupFlowRepository.findFirstByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(Optional.of(noTrackFlow));

    assertEquals(Track.EL, service.currentTrack(studentId));
  }

  @Test
  void currentTrack_returns_null_before_s4_when_group_has_no_track() {
    int entryYear = SemesterCalculator.entryYearFor(Semester.S1, LocalDate.now());
    var noTrackPromotion =
        JPromotion.builder().id(UUID.randomUUID()).ref("P").name("P").entryYear(entryYear).build();
    var noTrackGroup = JGroup.builder().id(groupId).ref("G1").track(null).build();
    var noTrackFlow =
        JGroupFlow.builder()
            .id(UUID.randomUUID())
            .student(student)
            .group(noTrackGroup)
            .groupFlowType(GroupFlowType.JOIN)
            .build();
    student.setPromotion(noTrackPromotion);
    given(studentRepository.findById(studentId)).willReturn(Optional.of(student));
    given(groupFlowRepository.findFirstByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(Optional.of(noTrackFlow));

    assertNull(service.currentTrack(studentId));
  }

  @Test
  void currentTrack_returns_null_when_student_has_no_promotion() {
    var noTrackGroup = JGroup.builder().id(groupId).ref("G1").track(null).build();
    var noTrackFlow =
        JGroupFlow.builder()
            .id(UUID.randomUUID())
            .student(student)
            .group(noTrackGroup)
            .groupFlowType(GroupFlowType.JOIN)
            .build();
    student.setPromotion(null);
    given(studentRepository.findById(studentId)).willReturn(Optional.of(student));
    given(groupFlowRepository.findFirstByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(Optional.of(noTrackFlow));

    assertNull(service.currentTrack(studentId));
  }

  @Test
  void currentTracks_maps_student_to_latest_join_track() {
    var otherStudentId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    var otherStudent =
        JStudent.builder()
            .id(otherStudentId)
            .firstname("Alan")
            .lastname("Turing")
            .std("STD24002")
            .promotion(promotion)
            .build();
    var leaveFlow =
        JGroupFlow.builder()
            .id(UUID.randomUUID())
            .student(student)
            .group(group)
            .groupFlowType(GroupFlowType.LEAVE)
            .build();
    leaveFlow.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
    var olderJoin =
        JGroupFlow.builder()
            .id(UUID.randomUUID())
            .student(student)
            .group(group)
            .groupFlowType(GroupFlowType.JOIN)
            .build();
    olderJoin.setCreatedAt(Instant.parse("2024-06-01T00:00:00Z"));
    var newerLeave =
        JGroupFlow.builder()
            .id(UUID.randomUUID())
            .student(student)
            .group(group)
            .groupFlowType(GroupFlowType.LEAVE)
            .build();
    newerLeave.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
    given(groupFlowRepository.findByStudentIdIn(List.of(studentId, otherStudentId)))
        .willReturn(List.of(joinFlow, olderJoin, leaveFlow, newerLeave));

    var result = service.currentTracks(List.of(student, otherStudent));

    assertEquals(Track.EL, result.get(studentId));
    assertNull(result.get(otherStudentId));
  }

  @Test
  void computeResultsSummaries_returns_summary_per_student() {
    var assignment2 =
        JCourseAssignment.builder()
            .id(UUID.randomUUID())
            .course(course)
            .group(group)
            .academicYear(2024)
            .semester(Semester.S3)
            .credits(6)
            .build();
    var assignment3 =
        JCourseAssignment.builder()
            .id(UUID.randomUUID())
            .course(course)
            .group(group)
            .academicYear(2024)
            .semester(Semester.S5)
            .credits(6)
            .build();
    var exam2 =
        JExam.builder()
            .id(UUID.randomUUID())
            .courseAssignment(assignment2)
            .title("Exam2")
            .examDatetime(Instant.parse("2025-03-01T08:00:00Z"))
            .build();
    exam2.setCoefficientFraction(new Fraction(1, 1));
    var exam3 =
        JExam.builder()
            .id(UUID.randomUUID())
            .courseAssignment(assignment3)
            .title("Exam3")
            .examDatetime(Instant.parse("2025-10-01T08:00:00Z"))
            .build();
    exam3.setCoefficientFraction(new Fraction(1, 1));
    var grade2 =
        JGrade.builder()
            .id(UUID.randomUUID())
            .student(student)
            .exam(exam2)
            .value(new BigDecimal("14.00"))
            .build();
    var grade3 =
        JGrade.builder()
            .id(UUID.randomUUID())
            .student(student)
            .exam(exam3)
            .value(new BigDecimal("14.00"))
            .build();
    given(groupFlowRepository.findByStudentIdIn(List.of(studentId))).willReturn(List.of(joinFlow));
    given(gradeRepository.findByStudentIdIn(List.of(studentId)))
        .willReturn(List.of(grade, grade2, grade3));
    given(courseAssignmentRepository.findCurriculumCourses(anyCollection(), anyCollection()))
        .willReturn(List.of(course));
    given(courseAssignmentRepository.findByGroupIdInAndSemesterIn(anyCollection(), anyCollection()))
        .willReturn(List.of(assignment, assignment2, assignment3));
    given(
            examRepository.findByCourseIdsAndSemestersAndGroups(
                anyCollection(), anyCollection(), anyCollection()))
        .willReturn(List.of(exam, exam2, exam3));

    Map<UUID, ResultsSummaryResponse> result = service.computeResultsSummaries(List.of(student));

    assertEquals(1, result.size());
    var summary = result.get(studentId);
    assertTrue(summary.graduate());
    assertEquals("STD24001", summary.studentStd());
  }

  @Test
  void computeResultsSummaries_handles_student_without_group() {
    given(groupFlowRepository.findByStudentIdIn(List.of(studentId))).willReturn(List.of());
    given(gradeRepository.findByStudentIdIn(List.of(studentId))).willReturn(List.of());
    given(courseAssignmentRepository.findCurriculumCourses(anyCollection(), anyCollection()))
        .willReturn(List.of());
    given(courseAssignmentRepository.findByGroupIdInAndSemesterIn(anyCollection(), anyCollection()))
        .willReturn(List.of());
    given(
            examRepository.findByCourseIdsAndSemestersAndGroups(
                anyCollection(), anyCollection(), anyCollection()))
        .willReturn(List.of());

    Map<UUID, ResultsSummaryResponse> result = service.computeResultsSummaries(List.of(student));

    assertEquals(1, result.size());
    assertEquals(Boolean.FALSE, result.get(studentId).graduate());
  }
}
