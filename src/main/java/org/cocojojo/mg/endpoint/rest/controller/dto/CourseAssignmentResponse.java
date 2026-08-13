package org.cocojojo.mg.endpoint.rest.controller.dto;

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
    UUID teacherId,
    String teacherFullName,
    int academicYear,
    Semester semester,
    int credits) {}
