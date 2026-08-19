package org.cocojojo.mg.endpoint.rest.controller;

import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.service.PromotionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PromotionViewController {

  private final PromotionService promotionService;

  @GetMapping("/ui/promotions")
  public String promotions(Model model) {
    model.addAttribute("promotions", promotionService.getAllWithoutPagination());
    return "promotions";
  }
}
