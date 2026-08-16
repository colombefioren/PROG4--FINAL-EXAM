package org.cocojojo.mg.endpoint.rest.controller;

import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.mapper.PromotionMapper;
import org.cocojojo.mg.repository.PromotionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PromotionViewController {

  private final PromotionRepository promotionRepository;
  private final PromotionMapper promotionMapper;

  @GetMapping("/ui/promotions")
  public String promotions(Model model) {
    var promotions = promotionRepository.findAll().stream().map(promotionMapper::toModel).toList();
    model.addAttribute("promotions", promotions);
    return "promotions";
  }
}
