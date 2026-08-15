package org.cocojojo.mg.endpoint.rest.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentRequest(
    UUID id,
    @NotBlank String std,
    @NotBlank String firstname,
    @NotBlank String lastname,
    @NotBlank @Email String email,
    String password,
    @NotNull UUID promotionId,
    UUID groupId) {}
