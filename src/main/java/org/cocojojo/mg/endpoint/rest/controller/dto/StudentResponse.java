package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentResponse(
    UUID id,
    String std,
    String firstname,
    String lastname,
    String email,
    UUID promotionId,
    String promotionName,
    UUID currentGroupId,
    String currentGroupRef) {}
