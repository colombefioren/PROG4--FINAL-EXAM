package org.cocojojo.mg.endpoint.rest.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record MoveStudentGroupRequest(@NotNull UUID studentId, @NotNull UUID groupId) {}
