package org.cocojojo.mg.endpoint.rest.controller;

import java.util.List;
import java.util.UUID;

import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionResponse;
import org.cocojojo.mg.service.PromotionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {
  private final PromotionService service;

  @GetMapping
  public List<PromotionResponse> getAll() {
    return service.getAll();
  }

  @GetMapping("/:id")
  public PromotionResponse getById(@PathVariable UUID id) {
    return service.getById(id);
  }

  @PutMapping
  public PromotionResponse upsert(@Valid @RequestBody PromotionRequest request) {
    return service.upsert(request);
  }
}
