package org.cocojojo.mg.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupFlowResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.MoveStudentGroupRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.ResultsSummaryResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.StudentRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.StudentResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.YearlyResultResponse;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.service.GroupFlowService;
import org.cocojojo.mg.service.ResultService;
import org.cocojojo.mg.service.StudentService;
import org.cocojojo.mg.service.TranscriptService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {
  private final StudentService studentService;
  private final GroupFlowService groupFlowService;
  private final TranscriptService transcriptService;
  private final ResultService resultService;

  @GetMapping
  public Page<StudentResponse> getAll(Pageable pageable) {
    return studentService.getAll(pageable);
  }

  @GetMapping("/{id}")
  public StudentResponse getById(@PathVariable UUID id) {
    return studentService.getById(id);
  }

  @PutMapping
  public StudentResponse upsert(@RequestBody @Valid StudentRequest request) {
    return studentService.upsert(request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    studentService.delete(id);
  }

  @GetMapping("/{id}/group-flows")
  public List<GroupFlowResponse> getGroupFlows(@PathVariable UUID id) {
    studentService.assertAdminOrSelf(id);
    return groupFlowService.getHistory(id);
  }

  @PutMapping("/{id}/group-flows")
  public GroupFlowResponse moveToGroup(
      @PathVariable UUID id, @RequestBody @Valid MoveStudentGroupRequest request) {
    return groupFlowService.move(studentService.getEntityOrThrow(id), request);
  }

  @PostMapping("/{id}/yearly-results/{level}/transcript")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void requestTranscript(@PathVariable UUID id, @PathVariable StudentLevel level) {
    studentService.assertAdminOrSelf(id);
    transcriptService.requestTranscript(id, level);
  }

  @GetMapping("/{id}/yearly-results/{level}")
  public YearlyResultResponse getYearlyResult(
      @PathVariable UUID id, @PathVariable StudentLevel level) {
    studentService.assertAdminOrSelf(id);
    return resultService.computeYearlyResult(id, level);
  }

  @GetMapping("/{id}/results-summary")
  public ResultsSummaryResponse getResultsSummary(@PathVariable UUID id) {
    studentService.assertAdminOrSelf(id);
    return resultService.computeResultsSummary(id);
  }
}
