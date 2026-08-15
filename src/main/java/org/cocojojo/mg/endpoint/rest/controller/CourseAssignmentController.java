package org.cocojojo.mg.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentResponse;
import org.cocojojo.mg.service.CourseAssignmentService;
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
@RequestMapping("/course-assignments")
public class CourseAssignmentController {

  private final CourseAssignmentService service;

  @GetMapping
  public List<CourseAssignmentResponse> getByFilter(
      @RequestParam(value = "group_id", required = false) UUID groupId,
      @RequestParam(value = "teacher_id", required = false) UUID teacherId,
      @RequestParam(value = "course_id", required = false) UUID courseId,
      @RequestParam(value = "academic_year", required = false) Integer academicYear) {
    return service.getByFilter(groupId, teacherId, courseId, academicYear);
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
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
