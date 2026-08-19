package org.cocojojo.mg.endpoint.rest.controller;

import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionResponse;
import org.cocojojo.mg.service.GraduateListService;
import org.cocojojo.mg.service.PromotionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PromotionViewController {

  private final PromotionService promotionService;
  private final GraduateListService graduateListService;

  @GetMapping("/ui/promotions")
  public String promotions(Model model) {
    var promotions = promotionService.getAllWithoutPagination();
    var graduateEligibility =
        promotions.stream()
            .collect(
                Collectors.toMap(
                    PromotionResponse::id, p -> graduateListService.isAcrossThreeYears(p.id())));
    model.addAttribute("promotions", promotions);
    model.addAttribute("graduateEligibility", graduateEligibility);
    return "promotions";
  }
}
