package org.cocojojo.mg.model;

import java.util.UUID;
import lombok.Builder;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;

@Builder
public record Course(
    UUID id,
    String code,
    String name,
    Integer credits,
    Integer totalHours,
    StudentLevel studentLevel,
    Track track) {}
