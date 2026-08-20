package org.cocojojo.mg.validator;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.model.Course;
import org.cocojojo.mg.model.CourseAssignment;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.model.Teacher;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GradeValidatorTest {

  private GradeValidator validator;

  private final UUID teacherId = UUID.fromString("66666666-6666-6666-6666-666666666666");

  @BeforeEach
  void setUp() {
    validator = new GradeValidator();
  }

  private CourseAssignment assignment(List<Teacher> teachers) {
    return CourseAssignment.builder()
        .id(UUID.randomUUID())
        .course(
            Course.builder()
                .id(UUID.randomUUID())
                .code("ALG1")
                .studentLevel(StudentLevel.L1)
                .track(Track.EL)
                .build())
        .group(Group.builder().id(UUID.randomUUID()).ref("G1").track(Track.EL).build())
        .teachers(teachers)
        .academicYear(2025)
        .semester(Semester.S1)
        .credits(6)
        .build();
  }

  @Test
  void validateTeacherTeaches_accepts_teacher_of_the_assignment() {
    var teacher = Teacher.builder().id(teacherId).firstname("Ada").lastname("Lovelace").build();

    validator.validateTeacherTeaches(teacherId, assignment(List.of(teacher)));
  }

  @Test
  void validateTeacherTeaches_throws_for_unknown_teacher() {
    var otherTeacher =
        Teacher.builder().id(UUID.randomUUID()).firstname("Alan").lastname("Turing").build();

    assertThrows(
        ForbiddenAccessException.class,
        () -> validator.validateTeacherTeaches(teacherId, assignment(List.of(otherTeacher))));
  }

  @Test
  void validateTeacherTeaches_throws_when_assignment_has_no_teachers() {
    assertThrows(
        ForbiddenAccessException.class,
        () -> validator.validateTeacherTeaches(teacherId, assignment(List.of())));
  }
}
