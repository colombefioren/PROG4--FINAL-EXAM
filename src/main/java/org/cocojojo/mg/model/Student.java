package org.cocojojo.mg.model;

import java.util.UUID;
import lombok.Builder;

@Builder
public record Student(
    UUID id,
    String firstname,
    String lastname,
    String email,
    String password,
    String std,
    Promotion promotion,
    Group currentGroup) {}
