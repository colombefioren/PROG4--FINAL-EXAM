package org.cocojojo.mg.mapper;

import lombok.AllArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.StudentResponse;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.model.Student;
import org.cocojojo.mg.repository.model.JStudent;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class StudentMapper {

  private final PromotionMapper promotionMapper;

  public Student toModel(JStudent entity, Group currentGroup) {
    return Student.builder()
        .id(entity.getId())
        .firstname(entity.getFirstname())
        .lastname(entity.getLastname())
        .email(entity.getEmail())
        .password(entity.getPassword())
        .std(entity.getStd())
        .promotion(promotionMapper.toModel(entity.getPromotion()))
        .currentGroup(currentGroup)
        .build();
  }

  public StudentResponse toResponse(Student model) {
    return StudentResponse.builder()
        .id(model.id())
        .std(model.std())
        .firstname(model.firstname())
        .lastname(model.lastname())
        .email(model.email())
        .promotionId(model.promotion().id())
        .promotionName(model.promotion().name())
        .currentGroupId(model.currentGroup() == null ? null : model.currentGroup().id())
        .currentGroupRef(model.currentGroup() == null ? null : model.currentGroup().ref())
        .build();
  }
}
