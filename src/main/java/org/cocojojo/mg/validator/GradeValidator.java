package org.cocojojo.mg.validator;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.model.CourseAssignment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GradeValidator {

  public void validateTeacherTeaches(UUID teacherId, CourseAssignment assignment) {
    boolean teaches =
        assignment.teachers().stream().anyMatch(teacher -> teacher.id().equals(teacherId));
    if (!teaches) {
      throw new ForbiddenAccessException("You may only manage grades for courses you teach");
    }
  }

  public void validateIsStudentSelf(UUID studentId) {
    if (!securityUtil.getCurrentUserIdOrThrow().equals(studentId)) {
      throw new ForbiddenAccessException("You may only access your own grades");
    }
  }

  public void validateStudentOwnsGrade(UUID studentId, Grade grade) {
    if (!grade.student().id().equals(studentId)) {
      throw new ForbiddenAccessException("This grade is not yours");
    }
  }
}
