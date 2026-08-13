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
    fractionValidator.accept(examInfo.coefficient());
    return JExam.builder()
        .id(examInfo.id())
        .courseAssignment(courseAssignment)
        .title(examInfo.title())
        .examDatetime(examInfo.examDatetime())
        .coefficientNumerator(examInfo.coefficient().numerator())
        .coefficientDenominator(examInfo.coefficient().denominator())
        .build();
  }
}
