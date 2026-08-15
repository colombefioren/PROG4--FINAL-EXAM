package org.cocojojo.mg.endpoint.rest.security;

import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.Role;

@Builder
public record AuthenticatedAccount(
    UUID id,
    String firstname,
    String lastname,
    String email,
    String password,
    boolean enabled,
    Role role) {}
