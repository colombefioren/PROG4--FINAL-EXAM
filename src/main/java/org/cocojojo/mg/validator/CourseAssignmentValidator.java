package org.cocojojo.mg.validator;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.model.Course;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JTeacher;
import org.cocojojo.mg.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseAssignmentValidator {

  private static final int MAX_CREDITS_PER_SEMESTER = 30;

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

  public void validateTrackCompatibility(Course course, Group group) {
    if (course.track() != null && group.track() != null && course.track() != group.track()) {
      throw new IllegalArgumentException(
          "Course "
              + course.code()
              + " belongs to track "
              + course.track()
              + " but the group "
              + group.ref()
              + " is on track "
              + group.track());
    }
  }

  public void validateCreditCeiling(List<JCourseAssignment> assignments) {
    int totalCredits = assignments.stream().mapToInt(JCourseAssignment::getCredits).sum();
    if (totalCredits > MAX_CREDITS_PER_SEMESTER) {
      throw new IllegalArgumentException(
          "Total credits "
              + totalCredits
              + " exceed the "
              + MAX_CREDITS_PER_SEMESTER
              + "-credit ceiling for one semester");
    }
  }

  public int creditsPerSemester() {
    return MAX_CREDITS_PER_SEMESTER;
  }
}