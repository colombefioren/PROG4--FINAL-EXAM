package org.cocojojo.mg.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentResponse;
import org.cocojojo.mg.service.CourseAssignmentService;
import org.cocojojo.mg.util.SecurityUtil;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/course-assignments")
public class CourseAssignmentController {

  private final CourseAssignmentService service;
  private final SecurityUtil securityUtil;

  @GetMapping
  public List<CourseAssignmentResponse> getByFilter(
      @RequestParam(value = "group_id", required = false) UUID groupId,
      @RequestParam(value = "teacher_id", required = false) UUID teacherId) {
    if (securityUtil.isTeacher()) {
      return service.getByTeacher(securityUtil.getCurrentUserIdOrThrow());
    }
    if (groupId != null) {
      return service.getByGroup(groupId);
    }
    if (teacherId != null) {
      return service.getByTeacher(teacherId);
    }
    throw new IllegalArgumentException("group_id or teacher_id is required");
  }

  @GetMapping("/{id}")
  public CourseAssignmentResponse getById(@PathVariable UUID id) {
    return service.getById(id);
  }

  /** Upsert only: existing assignments not present in the payload are left untouched. */
  @PutMapping
  public List<CourseAssignmentResponse> crupdate(
      @Valid @RequestBody List<CourseAssignmentRequest> requests) {
    return service.crupdate(requests);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
