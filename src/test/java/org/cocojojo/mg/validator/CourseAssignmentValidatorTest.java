package org.cocojojo.mg.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentRequest;
import org.cocojojo.mg.endpoint.rest.controller.exception.InvalidCurriculumException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.model.Course;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.UserRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JTeacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseAssignmentValidatorTest {

  @Mock private CourseAssignmentRepository courseAssignmentRepository;
  @Mock private UserRepository userRepository;

  private CourseAssignmentValidator validator;

  private final UUID courseId = UUID.randomUUID();
  private final UUID groupId = UUID.randomUUID();
  private final UUID teacherId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    validator = new CourseAssignmentValidator(courseAssignmentRepository, userRepository);
  }

  private JTeacher teacher() {
    return JTeacher.builder()
        .id(teacherId)
        .firstname("Ada")
        .lastname("Lovelace")
        .email("a@hei.school")
        .build();
  }

  private CourseAssignmentRequest assignmentRequest(
      UUID id, int credits, UUID groupId, int academicYear, Semester semester) {
    return CourseAssignmentRequest.builder()
        .id(id)
        .courseId(courseId)
        .groupId(groupId)
        .teacherIds(List.of(teacherId))
        .academicYear(academicYear)
        .semester(semester)
        .credits(credits)
        .build();
  }

  @Test
  void validateAllAreTeachers_accepts_when_all_users_are_teachers() {
    given(userRepository.findById(teacherId)).willReturn(Optional.of(teacher()));

    validator.validateAllAreTeachers(List.of(teacherId));
  }

  @Test
  void validateAllAreTeachers_throws_not_found_when_user_missing() {
    given(userRepository.findById(teacherId)).willReturn(Optional.empty());

    var ex =
        assertThrows(
            ResourceNotFoundException.class,
            () -> validator.validateAllAreTeachers(List.of(teacherId)));

    assertEquals("User with id:" + teacherId + " not found.", ex.getMessage());
  }

  @Test
  void validateAllAreTeachers_throws_when_user_is_not_a_teacher() {
    var admin = JAdmin.builder().id(teacherId).build();
    given(userRepository.findById(teacherId)).willReturn(Optional.of(admin));

    var ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateAllAreTeachers(List.of(teacherId)));

    assertEquals("User " + teacherId + " is not a teacher", ex.getMessage());
  }

  @Test
  void validateIsTeacher_accepts_teacher() {
    validator.validateIsTeacher(teacher());
  }

  @Test
  void validateIsTeacher_rejects_admin() {
    var admin = JAdmin.builder().id(teacherId).build();

    assertThrows(IllegalArgumentException.class, () -> validator.validateIsTeacher(admin));
  }

  @Test
  void validateCurriculum_accepts_matching_level_and_track() {
    var course =
        Course.builder()
            .id(courseId)
            .code("ALG1")
            .studentLevel(StudentLevel.L1)
            .track(null)
            .build();
    var group = Group.builder().id(groupId).ref("G1").track(null).build();

    assertDoesNotThrow(() -> validator.validateCurriculum(course, group, Semester.S1));
  }

  @Test
  void validateCurriculum_throws_on_track_mismatch() {
    var course =
        Course.builder()
            .id(courseId)
            .code("TN1")
            .studentLevel(StudentLevel.L1)
            .track(Track.TN)
            .build();
    var group = Group.builder().id(groupId).ref("G1").track(Track.EL).build();

    var ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateCurriculum(course, group, Semester.S1));

    assertEquals("Course TN1 belongs to track TN but the group G1 is on track EL", ex.getMessage());
  }

  @Test
  void validateCurriculum_throws_on_level_mismatch() {
    var course =
        Course.builder()
            .id(courseId)
            .code("ALG3")
            .studentLevel(StudentLevel.L3)
            .track(null)
            .build();
    var group = Group.builder().id(groupId).ref("G1").track(null).build();

    var ex =
        assertThrows(
            InvalidCurriculumException.class,
            () -> validator.validateCurriculum(course, group, Semester.S1));

    assertEquals("Course ALG3 is a L3 course, not compatible with S1", ex.getMessage());
  }

  @Test
  void validateCreditTargets_accepts_when_total_reaches_but_does_not_exceed_target() {
    var existing = JCourseAssignment.builder().id(UUID.randomUUID()).credits(24).build();
    given(
            courseAssignmentRepository.findByGroupIdAndAcademicYearAndSemester(
                groupId, 2025, Semester.S1))
        .willReturn(List.of(existing));
    var requests = List.of(assignmentRequest(null, 6, groupId, 2025, Semester.S1));

    validator.validateCreditTargets(requests);
  }

  @Test
  void validateCreditTargets_throws_when_total_exceeds_target() {
    var existing = JCourseAssignment.builder().id(UUID.randomUUID()).credits(24).build();
    given(
            courseAssignmentRepository.findByGroupIdAndAcademicYearAndSemester(
                groupId, 2025, Semester.S1))
        .willReturn(List.of(existing));
    var requests = List.of(assignmentRequest(null, 12, groupId, 2025, Semester.S1));

    var ex =
        assertThrows(
            IllegalArgumentException.class, () -> validator.validateCreditTargets(requests));

    assertEquals("Total credits 36 exceed the 30-credit target for one semester", ex.getMessage());
  }

  @Test
  void validateCreditTargets_ignores_replaced_assignments_in_existing_credits() {
    var replacedId = UUID.randomUUID();
    var replaced = JCourseAssignment.builder().id(replacedId).credits(24).build();
    var other = JCourseAssignment.builder().id(UUID.randomUUID()).credits(6).build();
    given(
            courseAssignmentRepository.findByGroupIdAndAcademicYearAndSemester(
                groupId, 2025, Semester.S1))
        .willReturn(List.of(replaced, other));
    var requests = List.of(assignmentRequest(replacedId, 12, groupId, 2025, Semester.S1));

    assertDoesNotThrow(() -> validator.validateCreditTargets(requests));
  }

  @Test
  void validateNotDuplicate_throws_when_duplicate_without_id() {
    given(
            courseAssignmentRepository.existsByCourseIdAndGroupIdAndAcademicYearAndSemester(
                courseId, groupId, 2025, Semester.S1))
        .willReturn(true);

    var ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateNotDuplicate(null, courseId, groupId, 2025, Semester.S1));

    assertEquals(
        "This course is already assigned to this group, for this academic year and semester",
        ex.getMessage());
  }

  @Test
  void validateNotDuplicate_allows_when_duplicate_but_updating() {
    given(
            courseAssignmentRepository.existsByCourseIdAndGroupIdAndAcademicYearAndSemester(
                courseId, groupId, 2025, Semester.S1))
        .willReturn(true);

    validator.validateNotDuplicate(UUID.randomUUID(), courseId, groupId, 2025, Semester.S1);
  }

  @Test
  void validateNotDuplicate_allows_when_no_duplicate() {
    given(
            courseAssignmentRepository.existsByCourseIdAndGroupIdAndAcademicYearAndSemester(
                courseId, groupId, 2025, Semester.S1))
        .willReturn(false);

    validator.validateNotDuplicate(null, courseId, groupId, 2025, Semester.S1);
  }

  @Test
  void validateCreditTarget_accepts_at_target() {
    validator.validateCreditTarget(30);
  }

  @Test
  void validateCreditTarget_throws_above_target() {
    var ex = assertThrows(IllegalArgumentException.class, () -> validator.validateCreditTarget(31));

    assertEquals("Total credits 31 exceed the 30-credit target for one semester", ex.getMessage());
  }

  @Test
  void targetCreditsPerSemester_returns_thirty() {
    assertEquals(30, validator.targetCreditsPerSemester());
  }
}
