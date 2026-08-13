package org.cocojojo.mg.model;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.Semester;

@Builder
public record CourseAssignment(
    UUID id,
    Course course,
    Group group,
    List<Teacher> teachers,
    Integer academicYear,
    Semester semester,
    Integer credits) {}
