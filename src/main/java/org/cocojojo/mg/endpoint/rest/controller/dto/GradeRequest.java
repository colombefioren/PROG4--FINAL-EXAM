package org.cocojojo.mg.endpoint.rest.controller.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
public record GradeRequest(
    @NotNull UUID studentId,
    @NotNull @DecimalMin("0") @DecimalMax("20") BigDecimal value,
    String comment) {}
