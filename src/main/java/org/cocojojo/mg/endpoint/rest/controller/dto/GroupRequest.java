package org.cocojojo.mg.endpoint.rest.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.Track;

@Builder
public record GroupRequest(UUID id, @NotNull UUID promotionId, @NotBlank String ref, Track track) {}
