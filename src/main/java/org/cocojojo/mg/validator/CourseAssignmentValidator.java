package org.cocojojo.mg.validator;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.model.JTeacher;
import org.cocojojo.mg.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseAssignmentValidator {

  private final CourseAssignmentRepository courseAssignmentRepository;

  public void validateIsTeacher(JUser user) {
    if (!(user instanceof JTeacher)) {
      throw new IllegalArgumentException("User " + user.getId() + " is not a teacher");
    }
  }

  public void validateNotDuplicate(
      UUID id, UUID courseId, UUID groupId, int academicYear, Semester semester) {
    boolean duplicate =
        courseAssignmentRepository.existsByCourseIdAndGroupIdAndAcademicYearAndSemester(
            courseId, groupId, academicYear, semester);
    if (duplicate && id == null) {
      throw new IllegalArgumentException(
          "This course is already assigned to this group, for this academic year and semester");
    }
  }
}
