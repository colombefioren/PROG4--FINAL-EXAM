package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.Track;

@Builder
public record GroupResponse(
    UUID id, UUID promotionId, String promotionName, String ref, Track track) {}
