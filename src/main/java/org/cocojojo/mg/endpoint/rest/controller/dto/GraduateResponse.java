package org.cocojojo.mg.endpoint.rest.controller.dto;

import java.math.BigDecimal;
import lombok.Builder;
import org.cocojojo.mg.model.enums.Track;

@Builder
public record GraduateResponse(
    int rank,
    String std,
    String firstname,
    String lastname,
    Track track,
    BigDecimal generalAverage) {}
