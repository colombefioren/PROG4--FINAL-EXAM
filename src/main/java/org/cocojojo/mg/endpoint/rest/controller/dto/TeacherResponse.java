package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.util.UUID;
import lombok.Builder;

@Builder
public record TeacherResponse(UUID id, String firstname, String lastname, String email) {}
