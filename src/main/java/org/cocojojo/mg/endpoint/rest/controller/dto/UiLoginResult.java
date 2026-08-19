package org.cocojojo.mg.endpoint.rest.controller.dto;

import lombok.Builder;

@Builder
public record UiLoginResult(String token, String redirectUrl) {}
