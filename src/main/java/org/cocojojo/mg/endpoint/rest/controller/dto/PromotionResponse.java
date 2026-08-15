package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.util.UUID;
import lombok.Builder;

@Builder
public record PromotionResponse(UUID id, String ref, String name, int entryYear) {}
