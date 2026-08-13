package org.cocojojo.mg.endpoint.rest.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PromotionRequest(
    UUID id, @NotBlank String ref, @NotBlank String name, @NotNull Integer entryYear) {}
