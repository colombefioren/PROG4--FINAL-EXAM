package org.cocojojo.mg.endpoint.rest.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Builder;

@Builder
public record TeacherRequest(
    UUID id,
    @NotBlank String firstname,
    @NotBlank String lastname,
    @NotBlank @Email String email,
    String password) {}
