package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.Track;

@Builder
public record CourseResultResponse(
    UUID courseId,
    String courseCode,
    String courseName,
    int credits,
    Track track,
    BigDecimal average,
    Boolean graded,
    Boolean complete,
    Boolean passed) {}
