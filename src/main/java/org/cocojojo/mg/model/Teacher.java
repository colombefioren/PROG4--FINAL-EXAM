package org.cocojojo.mg.model;

import java.util.UUID;
import lombok.Builder;

@Builder
public record Teacher(UUID id, String firstname, String lastname, String email, String password) {}