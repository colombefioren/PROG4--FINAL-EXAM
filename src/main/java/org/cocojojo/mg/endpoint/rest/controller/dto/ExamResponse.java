package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ExamResponse(
    UUID id, UUID courseAssignmentId, String title, Instant examDatetime, Fraction coefficient) {}
