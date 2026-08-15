package org.cocojojo.mg.validator;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.model.Fraction;
import org.cocojojo.mg.repository.ExamRepository;
import org.cocojojo.mg.repository.model.JExam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExamValidator {

  private final ExamRepository examRepository;

  public void validateCoefficient(
      UUID courseAssignmentId, UUID examIdBeingSaved, Fraction coefficient) {
    Fraction othersSum =
        examRepository.findByCourseAssignmentId(courseAssignmentId).stream()
            .filter(exam -> !exam.getId().equals(examIdBeingSaved))
            .map(JExam::getCoefficientFraction)
            .reduce(new Fraction(0, 1), Fraction::plus);

    Fraction total = othersSum.plus(coefficient);
    if (total.isGreaterThanOne()) {
      throw new IllegalArgumentException(
          "Sum of exam coefficients for this course assignment would exceed 1 (100%)");
    }
  }
}
