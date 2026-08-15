package org.cocojojo.mg.mapper;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeHistoryResponse;
import org.cocojojo.mg.model.GradeHistory;
import org.cocojojo.mg.repository.model.JGrade;
import org.cocojojo.mg.repository.model.JGradeHistory;
import org.cocojojo.mg.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GradeHistoryMapper {

  private final GradeMapper gradeMapper;
  private final UserMapper userMapper;

  public GradeHistory toModel(JGradeHistory entity) {
    return GradeHistory.builder()
        .id(entity.getId())
        .grade(gradeMapper.toModel(entity.getGrade()))
        .previousValue(entity.getPreviousValue())
        .newValue(entity.getNewValue())
        .reason(entity.getReason())
        .changedBy(userMapper.toModel(entity.getChangedBy()))
        .changedAt(entity.getChangedAt())
        .build();
  }

  public JGradeHistory toEntity(
      UUID id,
      JGrade grade,
      BigDecimal previousValue,
      BigDecimal newValue,
      String reason,
      JUser changedBy) {
    return JGradeHistory.builder()
        .id(id)
        .grade(grade)
        .previousValue(previousValue)
        .newValue(newValue)
        .reason(reason)
        .changedBy(changedBy)
        .build();
  }

  public GradeHistoryResponse toResponse(GradeHistory model) {
    return GradeHistoryResponse.builder()
        .id(model.id())
        .gradeId(model.grade().id())
        .previousValue(model.previousValue())
        .newValue(model.newValue())
        .reason(model.reason())
        .changedById(model.changedBy().id())
        .changedByName(model.changedBy().firstname() + " " + model.changedBy().lastname())
        .changedAt(model.changedAt())
        .build();
  }
}
