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
}
