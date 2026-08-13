package org.cocojojo.mg.endpoint.rest.controller.dto;

import lombok.AllArgsConstructor;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JExam;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ExamMapper {
  private final FractionValidator fractionValidator;

  public ExamResponse toRest(JExam exam) {
    return ExamResponse.builder()
        .id(exam.getId())
        .courseAssignmentId(exam.getCourseAssignment().getId())
        .title(exam.getTitle())
        .examDatetime(exam.getExamDatetime())
        .coefficient(Fraction.from(exam.getCoefficientFraction()))
        .build();
  }

  public JExam toDomain(ExamRequest examInfo, JCourseAssignment courseAssignment) {
    fractionValidator.accept(examInfo.getCoefficient());
    return JExam.builder()
        .id(examInfo.getId())
        .courseAssignment(courseAssignment)
        .title(examInfo.getTitle())
        .examDatetime(examInfo.getExamDatetime())
        .coefficientNumerator(examInfo.getCoefficient().numerator())
        .coefficientDenominator(examInfo.getCoefficient().denominator())
        .build();
  }
}
