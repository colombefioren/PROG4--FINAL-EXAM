package org.cocojojo.mg.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Exam(
    UUID id,
    CourseAssignment courseAssignment,
    String title,
    Instant examDatetime,
    Fraction coefficient) {}
