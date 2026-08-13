package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record GradeHistoryResponse(
    UUID id,
    UUID gradeId,
    BigDecimal previousValue,
    BigDecimal newValue,
    String reason,
    String changedBy,
    Instant changedAt) {}
