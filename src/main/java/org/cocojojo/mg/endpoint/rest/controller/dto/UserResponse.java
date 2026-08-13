package org.cocojojo.mg.endpoint.rest.controller.health.dto;

import java.util.UUID;
import lombok.Builder;

@Builder
public record UserResponse(UUID id, String firstname, String lastname, String email) {}
