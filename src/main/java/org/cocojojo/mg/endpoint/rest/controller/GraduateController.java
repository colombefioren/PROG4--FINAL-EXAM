package org.cocojojo.mg.endpoint.rest.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.GraduateExportResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.GraduateResponse;
import org.cocojojo.mg.service.GraduateListService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GraduateController {

  private final GraduateListService graduateListService;

  @GetMapping("/promotions/{promotionId}/graduates")
  public List<GraduateResponse> getGraduates(@PathVariable UUID promotionId) {
    return graduateListService.getGraduates(promotionId);
  }

  /** Generates the XLSX, uploads it to S3 and returns the pre-signed download URL. */
  @GetMapping("/promotions/{promotionId}/graduates/export")
  public GraduateExportResponse export(@PathVariable UUID promotionId) {
    return GraduateExportResponse.builder().url(graduateListService.export(promotionId)).build();
  }
}
