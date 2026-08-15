package org.cocojojo.mg.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.GroupFlowType;

@Builder
public record GroupFlow(
    UUID id, StudentSummary student, Group group, GroupFlowType groupFlowType, Instant createdAt) {}
