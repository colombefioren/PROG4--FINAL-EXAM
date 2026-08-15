package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.ResultStatus;
import org.cocojojo.mg.model.enums.Track;

/*
 * TODO: maybe instead of resultStatus we'll add the booleans graded, complete, passed
 * */
@Builder
public record CourseResultResponse(
    UUID courseId,
    String courseCode,
    String courseName,
    int credits,
    Track track,
    BigDecimal average,
    ResultStatus resultStatus,
    boolean passed) {}
