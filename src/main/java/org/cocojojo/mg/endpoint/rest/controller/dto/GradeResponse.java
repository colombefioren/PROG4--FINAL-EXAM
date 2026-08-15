package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record GradeResponse(
    UUID id,
    UUID studentId,
    String studentStd,
    UUID examId,
    String examTitle,
    String courseCode,
    BigDecimal value,
    String comment,
    Instant createdAt,
    Instant updatedAt) {}
