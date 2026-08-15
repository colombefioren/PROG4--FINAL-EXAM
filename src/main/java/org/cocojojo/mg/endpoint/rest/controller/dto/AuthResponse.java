package org.cocojojo.mg.endpoint.rest.controller.dto;

import lombok.Builder;

@Builder
public record AuthResponse(String token, UserResponse user) {}
