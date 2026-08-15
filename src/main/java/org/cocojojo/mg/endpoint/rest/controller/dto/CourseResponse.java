package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;

@Builder
public record CourseResponse(
    UUID id,
    String code,
    String name,
    Integer credits,
    Integer totalHours,
    StudentLevel studentLevel,
    Track track) {}
