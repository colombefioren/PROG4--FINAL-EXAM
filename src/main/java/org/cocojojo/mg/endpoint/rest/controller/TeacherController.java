package org.cocojojo.mg.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.TeacherRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.TeacherResponse;
import org.cocojojo.mg.service.TeacherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
public class TeacherController {
  private final TeacherService service;

  @GetMapping
  public List<TeacherResponse> getAll() {
    return service.getAll();
  }

  @GetMapping("/{id}")
  public TeacherResponse getById(@PathVariable UUID id) {
    return service.getById(id);
  }

  @PutMapping
  public TeacherResponse upsert(@RequestBody @Valid TeacherRequest request) {
    return service.upsert(request);
  }
}
