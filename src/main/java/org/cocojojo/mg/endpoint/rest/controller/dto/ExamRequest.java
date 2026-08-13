package org.cocojojo.mg.endpoint.rest.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ExamRequest(
    UUID id,
    @NotNull UUID courseAssignmentId,
    @NotBlank String title,
    @NotNull Instant examDatetime,
    @NotNull Fraction coefficient) {}
