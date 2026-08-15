package org.cocojojo.mg.mapper;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.ExamResponse;
import org.cocojojo.mg.model.Exam;
import org.cocojojo.mg.model.Fraction;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JExam;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ExamMapper {

  private final CourseAssignmentMapper courseAssignmentMapper;

  public Exam toModel(JExam entity) {
    return Exam.builder()
        .id(entity.getId())
        .courseAssignment(courseAssignmentMapper.toModel(entity.getCourseAssignment()))
        .title(entity.getTitle())
        .examDatetime(entity.getExamDatetime())
        .coefficient(entity.getCoefficientFraction())
        .build();
  }

  public JExam toEntity(
      UUID id,
      JCourseAssignment courseAssignment,
      String title,
      Instant examDatetime,
      Fraction coefficient) {
    var entity =
        JExam.builder()
            .id(id)
            .courseAssignment(courseAssignment)
            .title(title)
            .examDatetime(examDatetime)
            .build();
    entity.setCoefficientFraction(coefficient);
    return entity;
  }

  public ExamResponse toResponse(Exam model) {
    return ExamResponse.builder()
        .id(model.id())
        .courseAssignmentId(model.courseAssignment().id())
        .title(model.title())
        .examDatetime(model.examDatetime())
        .coefficient(model.coefficient())
        .build();
  }
}
