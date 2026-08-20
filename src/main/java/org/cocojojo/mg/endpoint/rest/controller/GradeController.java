package org.cocojojo.mg.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeCorrectionRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeDeleteRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeHistoryResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeResponse;
import org.cocojojo.mg.service.GradeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GradeController {

  private final GradeService gradeService;

  @GetMapping("/exams/{examId}/grades")
  public List<GradeResponse> getByExamId(@PathVariable UUID examId) {
    return gradeService.getByExamId(examId);
  }

  @PutMapping("/exams/{examId}/grades")
  public List<GradeResponse> create(
      @PathVariable UUID examId, @Valid @RequestBody List<GradeRequest> requests) {
    return gradeService.create(examId, requests);
  }

  @GetMapping("/exams/{examId}/students/{studentId}/grade")
  public GradeResponse getByExamIdAndStudentId(
      @PathVariable UUID examId, @PathVariable UUID studentId) {
    return gradeService.getByExamIdAndStudentId(examId, studentId);
  }

  @PatchMapping("/exams/{examId}/students/{studentId}/grade")
  public GradeResponse correct(
      @PathVariable UUID examId,
      @PathVariable UUID studentId,
      @Valid @RequestBody GradeCorrectionRequest request) {
    return gradeService.correct(examId, studentId, request);
  }

  @GetMapping("/students/{studentId}/grades")
  public List<GradeResponse> getByStudentId(@PathVariable UUID studentId) {
    return gradeService.getByStudentId(studentId);
  }

  @GetMapping("/grades/{gradeId}")
  public GradeResponse getById(@PathVariable UUID gradeId) {
    return gradeService.getById(gradeId);
  }

  @DeleteMapping("/grades/{gradeId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID gradeId, @Valid @RequestBody GradeDeleteRequest request) {
    gradeService.delete(gradeId, request.reason());
  }

  @GetMapping("/grades/{gradeId}/history")
  public List<GradeHistoryResponse> getHistory(@PathVariable UUID gradeId) {
    return gradeService.getHistory(gradeId);
  }
}
