package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.util.List;
import lombok.Builder;
import org.cocojojo.mg.model.enums.Semester;

@Builder
public record CurriculumStatusResponse(
    Semester semester,
    int assignedCredits,
    int targetCredits,
    boolean complete,
    List<CourseResponse> missingCourses,
    List<CourseAssignmentResponse> assignments) {}
