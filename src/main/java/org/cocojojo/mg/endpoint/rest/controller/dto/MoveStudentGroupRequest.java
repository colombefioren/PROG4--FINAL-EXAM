package org.cocojojo.mg.endpoint.rest.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

/*
 * TODO: we might want to remove the studentId field but it would depend on our endpoint, maybe we provide the user id in the endpoint
 *  the LEAVE should be untouched, only JOIN and LEAVE done behind the scenes
 * */
@Builder
public record MoveStudentGroupRequest(@NotNull UUID studentId, @NotNull UUID groupId) {}
