package org.cocojojo.mg.endpoint.rest.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResponse;
import org.cocojojo.mg.service.CourseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {
  private final CourseService service;

  @GetMapping
  public List<CourseResponse> getAll() {
    return service.getAll();
  }

  @GetMapping("/{id}")
  public CourseResponse getById(@PathVariable UUID id) {
    return service.getById(id);
  }

  @PutMapping
  public CourseResponse upsert(@RequestBody CourseRequest request) {
    return service.upsert(request);
  }
}
