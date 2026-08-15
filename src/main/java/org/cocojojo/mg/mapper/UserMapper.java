package org.cocojojo.mg.mapper;

import org.cocojojo.mg.endpoint.rest.controller.dto.UserResponse;
import org.cocojojo.mg.model.User;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public User toModel(JUser entity) {
    return User.builder()
        .id(entity.getId())
        .firstname(entity.getFirstname())
        .lastname(entity.getLastname())
        .email(entity.getEmail())
        .password(entity.getPassword())
        .build();
  }

  public UserResponse toResponse(User model, Role role) {
    return UserResponse.builder()
        .id(model.id())
        .firstname(model.firstname())
        .lastname(model.lastname())
        .email(model.email())
        .role(role)
        .build();
  }
}
