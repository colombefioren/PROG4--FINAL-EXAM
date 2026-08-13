package org.cocojojo.mg.model;

import java.util.UUID;
import lombok.Builder;

@Builder
public record Promotion(UUID id, String ref, String name, Integer entryYear) {}