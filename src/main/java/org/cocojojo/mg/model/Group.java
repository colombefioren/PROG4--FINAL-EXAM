package org.cocojojo.mg.model;

import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.Track;

@Builder
public record Group(UUID id, String ref, Track track, Promotion promotion) {}