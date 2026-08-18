package org.cocojojo.mg.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionResponse;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.service.PromotionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {
  private final PromotionService service;

  @GetMapping
  public Page<PromotionResponse> getAll(Pageable pageable) {
    return service.getAll(pageable);
  }

  @GetMapping("/{id}")
  public PromotionResponse getById(@PathVariable UUID id) {
    return service.getById(id);
  }

  @GetMapping("/{id}/courses")
  public List<CourseResponse> getCourses(
      @PathVariable UUID id,
      @RequestParam(required = false) StudentLevel studentLevel,
      @RequestParam(required = false) Track track) {
    return service.getCourses(id, studentLevel, track);
  }

  @PutMapping
  public PromotionResponse upsert(@Valid @RequestBody PromotionRequest request) {
    return service.upsert(request);
  }
}
