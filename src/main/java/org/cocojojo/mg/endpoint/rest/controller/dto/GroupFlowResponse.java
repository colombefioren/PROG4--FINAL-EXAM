package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.GroupFlowType;

/*
 * TODO: we might want to remove the studentId and studentStd fields but it would depend on our endpoint, maybe we provide them in the endpoint
 * */
@Builder
public record GroupFlowResponse(
    UUID id,
    UUID studentId,
    String studentStd,
    UUID groupId,
    String groupRef,
    GroupFlowType groupFlowType,
    Instant createdAt) {}
