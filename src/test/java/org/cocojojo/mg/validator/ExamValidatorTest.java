package org.cocojojo.mg.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.InvalidCurriculumException;
import org.cocojojo.mg.model.Course;
import org.cocojojo.mg.model.CourseAssignment;
import org.cocojojo.mg.model.Fraction;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.model.Teacher;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.ExamRepository;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.model.JExam;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JGroupFlow;
import org.cocojojo.mg.repository.model.JStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExamValidatorTest {

  @Mock private ExamRepository examRepository;
  @Mock private GroupFlowRepository groupFlowRepository;

  private ExamValidator validator;

  private final UUID courseAssignmentId = UUID.randomUUID();
  private final UUID examId = UUID.randomUUID();
  private final UUID teacherId = UUID.randomUUID();
  private final UUID studentId = UUID.randomUUID();
  private final UUID groupId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    validator = new ExamValidator(examRepository, groupFlowRepository);
  }

  private JExam exam(UUID id, Fraction coefficient) {
    var e = JExam.builder().id(id).title("T").build();
    e.setCoefficientFraction(coefficient);
    return e;
  }

  private CourseAssignment assignment(List<Teacher> teachers) {
    return assignment(teachers, groupId);
  }

  private CourseAssignment assignment(List<Teacher> teachers, UUID targetGroupId) {
    return CourseAssignment.builder()
        .id(courseAssignmentId)
        .course(
            Course.builder()
                .id(UUID.randomUUID())
                .code("ALG1")
                .studentLevel(StudentLevel.L1)
                .track(Track.EL)
                .build())
        .group(Group.builder().id(targetGroupId).ref("G1").track(Track.EL).build())
        .teachers(teachers)
        .academicYear(2025)
        .semester(Semester.S1)
        .credits(6)
        .build();
  }

  private Teacher teacher() {
    return Teacher.builder().id(teacherId).firstname("Ada").lastname("Lovelace").build();
  }

  @Test
  void validateCoefficient_accepts_when_sum_is_exactly_one() {
    given(examRepository.findByCourseAssignmentId(courseAssignmentId))
        .willReturn(List.of(exam(UUID.randomUUID(), new Fraction(1, 2))));

    validator.validateCoefficient(courseAssignmentId, null, new Fraction(1, 2));
  }

  @Test
  void validateCoefficient_accepts_when_sum_below_one() {
    given(examRepository.findByCourseAssignmentId(courseAssignmentId))
        .willReturn(List.of(exam(UUID.randomUUID(), new Fraction(1, 4))));

    validator.validateCoefficient(courseAssignmentId, null, new Fraction(1, 2));
  }

  @Test
  void validateCoefficient_throws_when_sum_exceeds_one() {
    given(examRepository.findByCourseAssignmentId(courseAssignmentId))
        .willReturn(List.of(exam(UUID.randomUUID(), new Fraction(3, 4))));

    var ex =
        assertThrows(
            InvalidCurriculumException.class,
            () -> validator.validateCoefficient(courseAssignmentId, null, new Fraction(1, 2)));

    assertEquals(
        "Sum of exam coefficients for this course assignment would exceed 1 (100%)",
        ex.getMessage());
  }

  @Test
  void validateCoefficient_ignores_the_exam_being_saved() {
    given(examRepository.findByCourseAssignmentId(courseAssignmentId))
        .willReturn(
            List.of(exam(examId, new Fraction(1, 2)), exam(UUID.randomUUID(), new Fraction(1, 2))));

    validator.validateCoefficient(courseAssignmentId, examId, new Fraction(1, 2));
  }

  @Test
  void validateTeacherTeaches_accepts_teacher_of_the_assignment() {
    validator.validateTeacherTeaches(teacherId, assignment(List.of(teacher())));
  }

  @Test
  void validateTeacherTeaches_throws_for_unknown_teacher() {
    var otherTeacher =
        Teacher.builder().id(UUID.randomUUID()).firstname("Alan").lastname("Turing").build();

    var ex =
        assertThrows(
            ForbiddenAccessException.class,
            () -> validator.validateTeacherTeaches(teacherId, assignment(List.of(otherTeacher))));

    assertEquals("You may only manage exams for courses you teach", ex.getMessage());
  }

  @Test
  void validateStudentInCurriculum_accepts_when_group_matches() {
    var flow =
        JGroupFlow.builder()
            .id(UUID.randomUUID())
            .student(JStudent.builder().id(studentId).build())
            .group(JGroup.builder().id(groupId).build())
            .groupFlowType(GroupFlowType.JOIN)
            .build();
    given(groupFlowRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(List.of(flow));

    validator.validateStudentInCurriculum(studentId, assignment(List.of()));
  }

  @Test
  void validateStudentInCurriculum_accepts_when_student_historically_belonged_to_group() {
    var pastAssignmentGroupId = UUID.randomUUID();
    var pastFlow =
        JGroupFlow.builder()
            .id(UUID.randomUUID())
            .student(JStudent.builder().id(studentId).build())
            .group(JGroup.builder().id(pastAssignmentGroupId).build())
            .groupFlowType(GroupFlowType.JOIN)
            .build();
    given(groupFlowRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(List.of(pastFlow));

    validator.validateStudentInCurriculum(studentId, assignment(List.of(), pastAssignmentGroupId));
  }

  @Test
  void validateStudentInCurriculum_throws_when_group_differs() {
    var flow =
        JGroupFlow.builder()
            .id(UUID.randomUUID())
            .student(JStudent.builder().id(studentId).build())
            .group(JGroup.builder().id(UUID.randomUUID()).build())
            .groupFlowType(GroupFlowType.JOIN)
            .build();
    given(groupFlowRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(List.of(flow));

    var ex =
        assertThrows(
            ForbiddenAccessException.class,
            () -> validator.validateStudentInCurriculum(studentId, assignment(List.of())));

    assertEquals("This exam is not part of your curriculum", ex.getMessage());
  }

  @Test
  void validateStudentInCurriculum_throws_when_no_join_flow() {
    var flow =
        JGroupFlow.builder()
            .id(UUID.randomUUID())
            .student(JStudent.builder().id(studentId).build())
            .group(JGroup.builder().id(groupId).build())
            .groupFlowType(GroupFlowType.LEAVE)
            .build();
    given(groupFlowRepository.findByStudentIdOrderByCreatedAtDesc(studentId))
        .willReturn(List.of(flow));

    assertThrows(
        ForbiddenAccessException.class,
        () -> validator.validateStudentInCurriculum(studentId, assignment(List.of())));
  }

  @Test
  void validateStudentInCurriculum_throws_when_no_flow_at_all() {
    given(groupFlowRepository.findByStudentIdOrderByCreatedAtDesc(studentId)).willReturn(List.of());

    var ex =
        assertThrows(
            ForbiddenAccessException.class,
            () -> validator.validateStudentInCurriculum(studentId, assignment(List.of())));

    assertEquals("This exam is not part of your curriculum", ex.getMessage());
  }
}
