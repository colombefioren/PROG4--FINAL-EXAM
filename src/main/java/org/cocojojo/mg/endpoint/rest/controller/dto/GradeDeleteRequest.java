package org.cocojojo.mg.endpoint.rest.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record GradeDeleteRequest(@NotBlank String reason) {}
