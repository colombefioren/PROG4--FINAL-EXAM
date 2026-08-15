package org.cocojojo.mg.endpoint.rest.controller.dto;

import lombok.Builder;
import org.cocojojo.mg.model.enums.Role;

@Builder
public record AuthResponse(
    String token, String userId, Role role, String firstName, String lastName) {}
