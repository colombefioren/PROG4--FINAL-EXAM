package org.cocojojo.mg.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.StudentResponse;
import org.cocojojo.mg.service.GroupService;
import org.cocojojo.mg.service.StudentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {
  private final GroupService service;
  private final StudentService studentService;

  @GetMapping
  public List<GroupResponse> getAll(@RequestParam(required = false) UUID promotionId) {
    return service.getAll(promotionId);
  }

  @PutMapping
  public GroupResponse upsert(@RequestBody @Valid GroupRequest request) {
    return service.upsert(request);
  }

  @GetMapping("/{id}/students")
  public List<StudentResponse> getStudents(@PathVariable UUID id) {
    return studentService.getByGroup(id);
  }
}
