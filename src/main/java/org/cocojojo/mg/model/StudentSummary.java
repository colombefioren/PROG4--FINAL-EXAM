package org.cocojojo.mg.model;

import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentSummary(
    UUID id, String firstname, String lastname, String email, String std) {}
