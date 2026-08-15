package org.cocojojo.mg.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Grade(
    UUID id,
    StudentSummary student,
    Exam exam,
    BigDecimal value,
    String comment,
    Instant createdAt,
    Instant updatedAt) {}
