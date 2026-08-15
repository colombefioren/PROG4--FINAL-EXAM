package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.Role;

@Builder
public record UserResponse(UUID id, String firstname, String lastname, String email, Role role) {}
