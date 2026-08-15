package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ResultsSummaryResponse(
    UUID studentId,
    String studentStd,
    List<YearlyResultResponse> levels,
    BigDecimal overallAverage,
    Boolean graduate) {}
