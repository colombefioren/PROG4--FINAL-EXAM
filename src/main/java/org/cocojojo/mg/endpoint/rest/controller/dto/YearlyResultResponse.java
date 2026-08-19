package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import org.cocojojo.mg.model.enums.ResultStatus;
import org.cocojojo.mg.model.enums.StudentLevel;

@Builder
public record YearlyResultResponse(
    StudentLevel level,
    List<CourseResultResponse> courses,
    BigDecimal overallAverage,
    int earnedCredits,
    int totalCredits,
    
    ResultStatus status) {}
