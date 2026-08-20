package org.cocojojo.mg.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.ExamRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.ExamResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.model.Fraction;
import org.cocojojo.mg.service.ExamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExamController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExamControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ExamService service;
  @MockBean private JwtService jwtService;

  private final UUID courseAssignmentId = UUID.randomUUID();
  private final UUID examId = UUID.randomUUID();

  private ExamResponse response() {
    return ExamResponse.builder()
        .id(examId)
        .courseAssignmentId(courseAssignmentId)
        .title("Midterm")
        .examDatetime(Instant.parse("2024-10-01T08:00:00Z"))
        .coefficient(new Fraction(1, 1))
        .build();
  }

  private String validBody() {
    return "{\"title\":\"Midterm\",\"examDatetime\":\"2024-10-01T08:00:00Z\","
        + "\"coefficient\":\"1/1\"}";
  }

  @Test
  void upsert_returns_created_exam() throws Exception {
    given(service.upsert(any(UUID.class), any(ExamRequest.class))).willReturn(response());

    mockMvc
        .perform(
            put("/course-assignments/{courseAssignmentId}/exams", courseAssignmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(examId.toString()))
        .andExpect(jsonPath("$.title").value("Midterm"))
        .andExpect(jsonPath("$.coefficient.numerator").value(1))
        .andExpect(jsonPath("$.coefficient.denominator").value(1));
  }

  @Test
  void upsert_returns_bad_request_when_title_blank() throws Exception {
    mockMvc
        .perform(
            put("/course-assignments/{courseAssignmentId}/exams", courseAssignmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"\",\"examDatetime\":\"2024-10-01T08:00:00Z\","
                        + "\"coefficient\":\"1/1\"}"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).upsert(any(UUID.class), any(ExamRequest.class));
  }

  @Test
  void upsert_returns_bad_request_when_coefficient_missing() throws Exception {
    mockMvc
        .perform(
            put("/course-assignments/{courseAssignmentId}/exams", courseAssignmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Midterm\",\"examDatetime\":\"2024-10-01T08:00:00Z\"}"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).upsert(any(UUID.class), any(ExamRequest.class));
  }

  @Test
  void getByCourseAssignmentId_returns_exams() throws Exception {
    given(service.getByCourseAssignmentId(courseAssignmentId, null, null))
        .willReturn(List.of(response()));

    mockMvc
        .perform(get("/course-assignments/{courseAssignmentId}/exams", courseAssignmentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(examId.toString()))
        .andExpect(jsonPath("$[0].title").value("Midterm"));
  }

  @Test
  void getByCourseAssignmentId_filters_by_date_range() throws Exception {
    given(service.getByCourseAssignmentId(any(UUID.class), any(Instant.class), any(Instant.class)))
        .willReturn(List.of(response()));

    mockMvc
        .perform(
            get("/course-assignments/{courseAssignmentId}/exams", courseAssignmentId)
                .param("from", "2024-01-01T00:00:00Z")
                .param("to", "2025-01-01T00:00:00Z"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].courseAssignmentId").value(courseAssignmentId.toString()));
  }

  @Test
  void getById_returns_exam() throws Exception {
    given(service.getById(courseAssignmentId, examId)).willReturn(response());

    mockMvc
        .perform(
            get("/course-assignments/{courseAssignmentId}/exams/{id}", courseAssignmentId, examId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(examId.toString()));
  }

  @Test
  void getById_returns_not_found_when_missing() throws Exception {
    given(service.getById(courseAssignmentId, examId))
        .willThrow(new ResourceNotFoundException("Exam with id:" + examId + " not found."));

    mockMvc
        .perform(
            get("/course-assignments/{courseAssignmentId}/exams/{id}", courseAssignmentId, examId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Exam with id:" + examId + " not found."));
  }

  @Test
  void delete_returns_no_content() throws Exception {
    mockMvc
        .perform(
            delete(
                "/course-assignments/{courseAssignmentId}/exams/{id}", courseAssignmentId, examId))
        .andExpect(status().isNoContent());

    then(service).should().delete(courseAssignmentId, examId);
  }
}
