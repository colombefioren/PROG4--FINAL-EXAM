package org.cocojojo.mg.mapper;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeResponse;
import org.cocojojo.mg.model.Grade;
import org.cocojojo.mg.repository.model.JExam;
import org.cocojojo.mg.repository.model.JGrade;
import org.cocojojo.mg.repository.model.JStudent;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GradeMapper {

  private final StudentMapper studentMapper;
  private final ExamMapper examMapper;

  public Grade toModel(JGrade entity) {
    return Grade.builder()
        .id(entity.getId())
        .student(studentMapper.toSummary(entity.getStudent()))
        .exam(examMapper.toModel(entity.getExam()))
        .value(entity.getValue())
        .comment(entity.getComment())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  public JGrade toEntity(UUID id, JStudent student, JExam exam, BigDecimal value, String comment) {
    return JGrade.builder()
        .id(id)
        .student(student)
        .exam(exam)
        .value(value)
        .comment(comment)
        .build();
  }

  public GradeResponse toResponse(Grade model) {
    return GradeResponse.builder()
        .id(model.id())
        .studentId(model.student().id())
        .studentStd(model.student().std())
        .examId(model.exam().id())
        .examTitle(model.exam().title())
        .courseCode(model.exam().courseAssignment().course().code())
        .value(model.value())
        .comment(model.comment())
        .createdAt(model.createdAt())
        .updatedAt(model.updatedAt())
        .build();
  }

  public GradeResponse toResponse(JGrade entity) {
    return toResponse(toModel(entity));
  }
}
