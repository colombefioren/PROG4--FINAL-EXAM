package org.cocojojo.mg.mapper;

import org.cocojojo.mg.endpoint.rest.controller.dto.TeacherResponse;
import org.cocojojo.mg.model.Teacher;
import org.cocojojo.mg.repository.model.JTeacher;
import org.springframework.stereotype.Component;

@Component
public class TeacherMapper {

  public Teacher toModel(JTeacher entity) {
    return Teacher.builder()
        .id(entity.getId())
        .firstname(entity.getFirstname())
        .lastname(entity.getLastname())
        .email(entity.getEmail())
        .password(entity.getPassword())
        .build();
  }

  public TeacherResponse toResponse(Teacher model) {
    return TeacherResponse.builder()
        .id(model.id())
        .firstname(model.firstname())
        .lastname(model.lastname())
        .email(model.email())
        .build();
  }
}
