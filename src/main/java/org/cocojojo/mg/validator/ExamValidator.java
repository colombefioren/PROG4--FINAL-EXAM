package org.cocojojo.mg.validator;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.InvalidCurriculumException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.model.CourseAssignment;
import org.cocojojo.mg.model.Fraction;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.repository.ExamRepository;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.model.JExam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExamValidator {

  private final ExamRepository examRepository;
  private final GroupFlowRepository groupFlowRepository;

  public void validateCoefficient(
      UUID courseAssignmentId, UUID examIdBeingSaved, Fraction coefficient) {
    long num = coefficient.numerator();
    long den = coefficient.denominator();
    for (JExam exam : examRepository.findByCourseAssignmentId(courseAssignmentId)) {
      if (exam.getId().equals(examIdBeingSaved)) {
        continue;
      }
      Fraction other = exam.getCoefficientFraction();
      long n = num * other.denominator() + other.numerator() * den;
      long d = den * other.denominator();
      long g = gcd(n, d);
      num = n / g;
      den = d / g;
    }
    if (num > den) {
      throw new InvalidCurriculumException(
          "Sum of exam coefficients for this course assignment would exceed 1 (100%)");
    }
  }

  private static long gcd(long a, long b) {
    a = Math.abs(a);
    b = Math.abs(b);
    while (b != 0) {
      long t = b;
      b = a % b;
      a = t;
    }
    return a;
  }

  public void validateTeacherTeaches(UUID teacherId, CourseAssignment assignment) {
    boolean teaches =
        assignment.teachers().stream().anyMatch(teacher -> teacher.id().equals(teacherId));
    if (!teaches) {
      throw new ForbiddenAccessException("You may only manage exams for courses you teach");
    }
  }

  public void validateStudentInCurriculum(UUID studentId, CourseAssignment assignment) {
    var groupId =
        groupFlowRepository
            .findFirstByStudentIdOrderByCreatedAtDesc(studentId)
            .filter(flow -> flow.getGroupFlowType() == GroupFlowType.JOIN)
            .map(flow -> flow.getGroup().getId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Student with id:" + studentId + " has no group"));
    if (!groupId.equals(assignment.group().id())) {
      throw new ForbiddenAccessException("This exam is not part of your curriculum");
    }
  }
}
