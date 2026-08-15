package org.cocojojo.mg.mapper;

import org.cocojojo.mg.endpoint.rest.controller.dto.AdminResponse;
import org.cocojojo.mg.model.Admin;
import org.cocojojo.mg.repository.model.JAdmin;
import org.springframework.stereotype.Component;

@Component
public class AdminMapper {

  public Admin toModel(JAdmin entity) {
    return Admin.builder()
        .id(entity.getId())
        .firstname(entity.getFirstname())
        .lastname(entity.getLastname())
        .email(entity.getEmail())
        .password(entity.getPassword())
        .build();
  }

  public AdminResponse toResponse(Admin model) {
    return AdminResponse.builder()
        .id(model.id())
        .firstname(model.firstname())
        .lastname(model.lastname())
        .email(model.email())
        .build();
  }
}
