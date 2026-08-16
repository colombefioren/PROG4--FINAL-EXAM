package org.cocojojo.mg.endpoint.rest.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.GraduateExportResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.GraduateResponse;
import org.cocojojo.mg.mapper.PromotionMapper;
import org.cocojojo.mg.repository.PromotionRepository;
import org.cocojojo.mg.service.GraduateListService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class GraduateController {

  private final GraduateListService graduateListService;
  private final PromotionRepository promotionRepository;
  private final PromotionMapper promotionMapper;

  @GetMapping("/promotions/{promotion_id}/graduates")
  @ResponseBody
  public List<GraduateResponse> getGraduates(@PathVariable("promotion_id") UUID promotionId) {
    return graduateListService.getGraduates(promotionId);
  }

  /** Generates the XLSX, uploads it to S3 and returns the pre-signed download URL. */
  @GetMapping("/promotions/{promotion_id}/graduates/export")
  @ResponseBody
  public GraduateExportResponse export(@PathVariable("promotion_id") UUID promotionId) {
    return GraduateExportResponse.builder().url(graduateListService.export(promotionId)).build();
  }

  @GetMapping("/ui/promotions")
  public String promotions(Model model) {
    var promotions = promotionRepository.findAll().stream().map(promotionMapper::toModel).toList();
    model.addAttribute("promotions", promotions);
    return "promotions";
  }
}
