package org.cocojojo.mg.endpoint.rest.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;

@Builder
public record CourseRequest(
    UUID id,
    @NotBlank String code,
    @NotBlank String name,
    @NotNull @Min(1) Integer credits,
    @Positive Integer totalHours,
    @NotNull StudentLevel studentLevel,
    Track track) {}
