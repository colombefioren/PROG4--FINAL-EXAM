package org.cocojojo.mg.endpoint.rest.controller.health.dto;

import lombok.Builder;

@Builder
public record AuthResponse(String token, UserResponse user) {}