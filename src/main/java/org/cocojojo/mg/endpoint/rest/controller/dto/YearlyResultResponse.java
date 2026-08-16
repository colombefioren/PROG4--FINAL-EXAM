package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import org.cocojojo.mg.model.enums.StudentLevel;

@Builder
public record YearlyResultResponse(
    StudentLevel level,
    List<CourseResultResponse> courses,
    BigDecimal overallAverage,
    int earnedCredits,
    int totalCredits,
    /** Whether every course of this level is complete (all its exams graded and passed). */
    Boolean complete) {}
