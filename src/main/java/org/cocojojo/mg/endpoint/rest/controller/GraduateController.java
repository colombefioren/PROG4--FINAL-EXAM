package org.cocojojo.mg.endpoint.rest.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.GraduateExportResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.GraduateResponse;
import org.cocojojo.mg.service.GraduateListService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GraduateController {

  private final GraduateListService graduateListService;

  @GetMapping("/promotions/{promotion_id}/graduates")
  public List<GraduateResponse> getGraduates(@PathVariable("promotion_id") UUID promotionId) {
    return graduateListService.getGraduates(promotionId);
  }

  @GetMapping("/promotions/{promotion_id}/graduates/export")
  public GraduateExportResponse export(@PathVariable("promotion_id") UUID promotionId) {
    return GraduateExportResponse.builder().url(graduateListService.export(promotionId)).build();
  }

  @GetMapping("/promotions/{promotion_id}/graduates/download")
  public ResponseEntity<byte[]> download(@PathVariable("promotion_id") UUID promotionId) {
    var bytes = graduateListService.buildXlsx(promotionId);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"graduates-" + promotionId + ".xlsx\"")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }
}
