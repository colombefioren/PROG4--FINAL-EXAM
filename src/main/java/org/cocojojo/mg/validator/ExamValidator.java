package org.cocojojo.mg.validator;

import java.math.BigInteger;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.InvalidCurriculumException;
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
    BigInteger num = BigInteger.valueOf(coefficient.numerator());
    BigInteger den = BigInteger.valueOf(coefficient.denominator());
    for (JExam exam : examRepository.findByCourseAssignmentId(courseAssignmentId)) {
      if (exam.getId().equals(examIdBeingSaved)) {
        continue;
      }
      Fraction other = exam.getCoefficientFraction();
      BigInteger od = BigInteger.valueOf(other.denominator());
      BigInteger on = BigInteger.valueOf(other.numerator());
      num = num.multiply(od).add(on.multiply(den));
      den = den.multiply(od);
      BigInteger g = num.gcd(den);
      num = num.divide(g);
      den = den.divide(g);
    }
    if (num.compareTo(den) > 0) {
      throw new InvalidCurriculumException(
          "Sum of exam coefficients for this course assignment would exceed 1 (100%)");
    }
  }

  public void validateTeacherTeaches(UUID teacherId, CourseAssignment assignment) {
    boolean teaches =
        assignment.teachers().stream().anyMatch(teacher -> teacher.id().equals(teacherId));
    if (!teaches) {
      throw new ForbiddenAccessException("You may only manage exams for courses you teach");
    }
  }

  public void validateStudentInCurriculum(UUID studentId, CourseAssignment assignment) {
    boolean belongedToCurrentOrPastGroup =
        groupFlowRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
            .filter(gf -> gf.getGroupFlowType() == GroupFlowType.JOIN)
            .anyMatch(gf -> gf.getGroup().getId().equals(assignment.group().id()));
    if (!belongedToCurrentOrPastGroup) {
      throw new ForbiddenAccessException("This exam is not part of your curriculum");
    }
  }
}
