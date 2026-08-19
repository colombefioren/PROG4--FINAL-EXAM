package org.cocojojo.mg.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.CurriculumStatusResponse;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.service.CourseAssignmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
  public Page<CourseAssignmentResponse> getByFilter(
      @RequestParam(required = false) UUID groupId,
      @RequestParam(required = false) UUID teacherId,
      @RequestParam(required = false) UUID courseId,
      @RequestParam(required = false) Integer academicYear,
      Pageable pageable) {
    return service.getByFilter(groupId, teacherId, courseId, academicYear, pageable);
  }

  @GetMapping("/{id}")
  public CourseAssignmentResponse getById(@PathVariable UUID id) {
    return service.getById(id);
  }

  @GetMapping("/curriculum-status")
  public CurriculumStatusResponse curriculumStatus(
      @RequestParam UUID groupId, @RequestParam int academicYear, @RequestParam Semester semester) {
    return service.curriculumStatus(groupId, academicYear, semester);
  }

  // Bulk on purpose: building a curriculum means assigning many courses to a group at once,
  // and one request lets the validator check the per-semester credit ceiling across the batch.
  @PutMapping
  public List<CourseAssignmentResponse> upsert(
      @Valid @RequestBody List<CourseAssignmentRequest> requests) {
    return service.upsert(requests);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
