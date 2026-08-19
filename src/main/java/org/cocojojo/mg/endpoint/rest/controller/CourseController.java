package org.cocojojo.mg.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResponse;
import org.cocojojo.mg.service.CourseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {
  private final CourseService service;

  @GetMapping
  public Page<CourseResponse> getAll(Pageable pageable) {
    return service.getAll(pageable);
  }

  @GetMapping("/{id}")
  public CourseResponse getById(@PathVariable UUID id) {
    return service.getById(id);
  }

  @PutMapping
  public CourseResponse upsert(@RequestBody @Valid CourseRequest request) {
    return service.upsert(request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
