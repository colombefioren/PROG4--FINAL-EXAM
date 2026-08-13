package org.cocojojo.mg.endpoint.rest.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.Semester;

@Builder
public record CourseAssignmentRequest(
    UUID id,
    @NotNull UUID courseId,
    @NotNull UUID groupId,
    @NotNull UUID teacherId,
    @NotNull @Min(2000) @Max(2100) Integer academicYear,
    @NotNull Semester semester,
    @NotNull @Min(0) Integer credits) {}
