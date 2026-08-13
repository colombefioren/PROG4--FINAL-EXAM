package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.GroupFlowType;

@Builder
public record GroupFlowResponse(
    UUID id,
    UUID studentId,
    String studentStd,
    UUID groupId,
    String groupRef,
    GroupFlowType groupFlowType,
    Instant createdAt) {}
