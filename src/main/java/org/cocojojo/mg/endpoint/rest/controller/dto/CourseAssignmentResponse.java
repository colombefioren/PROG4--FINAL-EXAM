package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.Semester;

@Builder
public record CourseAssignmentResponse(
    UUID id,
    UUID courseId,
    String courseCode,
    String courseName,
    UUID groupId,
    String groupRef,
    List<TeacherResponse> teachers,
    Integer academicYear,
    Semester semester,
    Integer credits) {}
