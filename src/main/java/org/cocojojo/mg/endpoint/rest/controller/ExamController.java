package org.cocojojo.mg.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.ExamRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.ExamResponse;
import org.cocojojo.mg.service.ExamService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/course-assignments/{courseAssignmentId}/exams")
public class ExamController {

  private final ExamService service;

  @PutMapping
  public ExamResponse upsert(
      @PathVariable UUID courseAssignmentId, @Valid @RequestBody ExamRequest request) {
    return service.upsert(courseAssignmentId, request);
  }

  @GetMapping
  public List<ExamResponse> getByCourseAssignmentId(
      @PathVariable UUID courseAssignmentId,
      @RequestParam(value = "from", required = false) Instant from,
      @RequestParam(value = "to", required = false) Instant to) {
    return service.getByCourseAssignmentId(courseAssignmentId, from, to);
  }

  @GetMapping("/{id}")
  public ExamResponse getById(@PathVariable UUID courseAssignmentId, @PathVariable UUID id) {
    return service.getById(courseAssignmentId, id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID courseAssignmentId, @PathVariable UUID id) {
    service.delete(courseAssignmentId, id);
  }
}
