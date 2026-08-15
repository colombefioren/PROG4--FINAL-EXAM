package org.cocojojo.mg.endpoint.rest.controller.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record GradeCorrectionRequest(
    @NotNull @DecimalMin("0.0") @DecimalMax("20.0") BigDecimal value, @NotBlank String reason) {}
