package org.cocojojo.mg.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeCorrectionRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeHistoryResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeResponse;
import org.cocojojo.mg.service.GradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GradeController {

  private final GradeService gradeService;

  @GetMapping("/exams/{exam_id}/grades")
  public List<GradeResponse> getByExamId(@PathVariable("exam_id") UUID examId) {
    return gradeService.getByExamId(examId);
  }

  @PutMapping("/exams/{exam_id}/grades")
  public List<GradeResponse> upsert(
      @PathVariable("exam_id") UUID examId, @Valid @RequestBody List<GradeRequest> requests) {
    return gradeService.upsert(examId, requests);
  }

  @GetMapping("/exams/{exam_id}/students/{student_id}/grade")
  public GradeResponse getByExamIdAndStudentId(
      @PathVariable("exam_id") UUID examId, @PathVariable("student_id") UUID studentId) {
    return gradeService.getByExamIdAndStudentId(examId, studentId);
  }

  @PutMapping("/exams/{exam_id}/students/{student_id}/grade")
  public GradeResponse correct(
      @PathVariable("exam_id") UUID examId,
      @PathVariable("student_id") UUID studentId,
      @Valid @RequestBody GradeCorrectionRequest request) {
    return gradeService.correct(examId, studentId, request);
  }

  @GetMapping("/students/{student_id}/grades")
  public List<GradeResponse> getByStudentId(@PathVariable("student_id") UUID studentId) {
    return gradeService.getByStudentId(studentId);
  }

  @GetMapping("/grades/{grade_id}")
  public GradeResponse getById(@PathVariable("grade_id") UUID gradeId) {
    return gradeService.getById(gradeId);
  }

  @DeleteMapping("/grades/{grade_id}")
  public ResponseEntity<Void> delete(@PathVariable("grade_id") UUID gradeId) {
    gradeService.delete(gradeId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/grades/{grade_id}/history")
  public List<GradeHistoryResponse> getHistory(@PathVariable("grade_id") UUID gradeId) {
    return gradeService.getHistory(gradeId);
  }
}
