package org.cocojojo.mg.validator;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentRequest;
import org.cocojojo.mg.endpoint.rest.controller.exception.InvalidCurriculumException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.model.Course;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.UserRepository;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JTeacher;
import org.cocojojo.mg.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseAssignmentValidator {

  private static final int TARGET_CREDITS_PER_SEMESTER = 30;

  private final CourseAssignmentRepository courseAssignmentRepository;
  private final UserRepository userRepository;

  public void validateAllAreTeachers(List<UUID> teacherIds) {
    teacherIds.forEach(
        id -> {
          var user =
              userRepository
                  .findById(id)
                  .orElseThrow(
                      () -> new ResourceNotFoundException("User with id:" + id + " not found."));
          validateIsTeacher(user);
        });
  }

  public void validateIsTeacher(JUser user) {
    if (!(user instanceof JTeacher)) {
      throw new IllegalArgumentException("User " + user.getId() + " is not a teacher");
    }
  }

  public void validateCurriculum(Course course, Group group, Semester semester) {
    validateTrackCompatibility(course, group);
    if (course.studentLevel() != StudentLevel.of(semester)) {
      throw new InvalidCurriculumException(
          "Course "
              + course.code()
              + " is a "
              + course.studentLevel()
              + " course, not compatible with "
              + semester);
    }
  }

  public void validateCreditTargets(List<CourseAssignmentRequest> requests) {
    requests.stream()
        .map(r -> new GroupYearSemester(r.groupId(), r.academicYear(), r.semester()))
        .distinct()
        .forEach(triple -> validateCreditTarget(triple, requests));
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

  public void validateCreditTarget(int totalCredits) {
    if (totalCredits > TARGET_CREDITS_PER_SEMESTER) {
      throw new IllegalArgumentException(
          "Total credits "
              + totalCredits
              + " exceed the "
              + TARGET_CREDITS_PER_SEMESTER
              + "-credit target for one semester");
    }
  }

  public int targetCreditsPerSemester() {
    return TARGET_CREDITS_PER_SEMESTER;
  }

  private void validateTrackCompatibility(Course course, Group group) {
    // A track-specific course (e.g. TN1) must never be assigned to a group that is not on that
    // track — including an L1-shaped group whose track is null. Java's != already handles a null
    // group track correctly, so no extra guard is needed.
    if (course.track() != null && course.track() != group.track()) {
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

  private void validateCreditTarget(
      GroupYearSemester triple, List<CourseAssignmentRequest> requests) {
    var existing =
        courseAssignmentRepository.findByGroupIdAndAcademicYearAndSemester(
            triple.groupId(), triple.academicYear(), triple.semester());
    var replacedIds =
        requests.stream().filter(r -> r.id() != null).map(CourseAssignmentRequest::id).toList();
    int existingCredits =
        existing.stream()
            .filter(a -> !replacedIds.contains(a.getId()))
            .mapToInt(JCourseAssignment::getCredits)
            .sum();
    int incomingCredits =
        requests.stream()
            .filter(
                r ->
                    r.groupId().equals(triple.groupId())
                        && r.academicYear() == triple.academicYear()
                        && r.semester() == triple.semester())
            .mapToInt(CourseAssignmentRequest::credits)
            .sum();
    validateCreditTarget(existingCredits + incomingCredits);
  }

  private record GroupYearSemester(UUID groupId, int academicYear, Semester semester) {}
}
