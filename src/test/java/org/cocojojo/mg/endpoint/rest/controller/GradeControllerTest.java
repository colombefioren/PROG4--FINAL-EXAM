package org.cocojojo.mg.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeCorrectionRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeHistoryResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeResponse;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.service.GradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GradeController.class)
@AutoConfigureMockMvc(addFilters = false)
class GradeControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private GradeService service;
  @MockBean private JwtService jwtService;

  private final UUID gradeId = UUID.fromString("88888888-8888-8888-8888-888888888888");
  private final UUID examId = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private final UUID studentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  private GradeResponse response() {
    return GradeResponse.builder()
        .id(gradeId)
        .studentId(studentId)
        .studentStd("STD24001")
        .examId(examId)
        .examTitle("Midterm")
        .courseCode("ALG1")
        .value(new BigDecimal("14.50"))
        .comment("Good")
        .createdAt(Instant.parse("2024-10-05T08:00:00Z"))
        .updatedAt(Instant.parse("2024-10-05T08:00:00Z"))
        .build();
  }

  @Test
  void getByExamId_returns_grades() throws Exception {
    given(service.getByExamId(examId)).willReturn(List.of(response()));

    mockMvc
        .perform(get("/exams/{examId}/grades", examId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(gradeId.toString()))
        .andExpect(jsonPath("$[0].studentStd").value("STD24001"))
        .andExpect(jsonPath("$[0].value").value(14.5));
  }

  @Test
  void create_returns_grades() throws Exception {
    given(service.create(any(UUID.class), any())).willReturn(List.of(response()));

    mockMvc
        .perform(
            put("/exams/{examId}/grades", examId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "[{\"studentId\":\"" + studentId + "\",\"value\":14.5,\"comment\":\"Good\"}]"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].examTitle").value("Midterm"))
        .andExpect(jsonPath("$[0].value").value(14.5));
  }

  @Test
  void create_returns_bad_request_when_value_out_of_range() throws Exception {
    mockMvc
        .perform(
            put("/exams/{examId}/grades", examId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"studentId\":\"" + studentId + "\",\"value\":25}]"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).create(any(UUID.class), any());
  }

  @Test
  void create_returns_bad_request_when_student_id_missing() throws Exception {
    mockMvc
        .perform(
            put("/exams/{examId}/grades", examId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"value\":14.5}]"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).create(any(UUID.class), any());
  }

  @Test
  void getByExamIdAndStudentId_returns_grade() throws Exception {
    given(service.getByExamIdAndStudentId(examId, studentId)).willReturn(response());

    mockMvc
        .perform(get("/exams/{examId}/students/{studentId}/grade", examId, studentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studentId").value(studentId.toString()))
        .andExpect(jsonPath("$.value").value(14.5));
  }

  @Test
  void correct_returns_corrected_grade() throws Exception {
    given(service.correct(any(UUID.class), any(UUID.class), any(GradeCorrectionRequest.class)))
        .willReturn(response());

    mockMvc
        .perform(
            patch("/exams/{examId}/students/{studentId}/grade", examId, studentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":15.0,\"reason\":\"typo\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value(14.5));
  }

  @Test
  void correct_returns_bad_request_when_reason_blank() throws Exception {
    mockMvc
        .perform(
            patch("/exams/{examId}/students/{studentId}/grade", examId, studentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":15.0,\"reason\":\"\"}"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).correct(any(UUID.class), any(UUID.class), any());
  }

  @Test
  void getByStudentId_returns_grades() throws Exception {
    given(service.getByStudentId(studentId)).willReturn(List.of(response()));

    mockMvc
        .perform(get("/students/{studentId}/grades", studentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].studentId").value(studentId.toString()));
  }

  @Test
  void getById_returns_grade() throws Exception {
    given(service.getById(gradeId)).willReturn(response());

    mockMvc
        .perform(get("/grades/{gradeId}", gradeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(gradeId.toString()));
  }

  @Test
  void delete_returns_no_content_and_passes_reason() throws Exception {
    mockMvc
        .perform(
            delete("/grades/{gradeId}", gradeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"removed\"}"))
        .andExpect(status().isNoContent());

    then(service).should().delete(gradeId, "removed");
  }

  @Test
  void delete_returns_bad_request_when_reason_blank() throws Exception {
    mockMvc
        .perform(
            delete("/grades/{gradeId}", gradeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"\"}"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).delete(any(UUID.class), any(String.class));
  }

  @Test
  void getHistory_returns_history() throws Exception {
    var history =
        GradeHistoryResponse.builder()
            .id(UUID.randomUUID())
            .gradeId(gradeId)
            .previousValue(new BigDecimal("12.00"))
            .newValue(new BigDecimal("14.50"))
            .reason("typo")
            .changedById(studentId)
            .changedByName("Grace Hopper")
            .changedAt(Instant.parse("2024-10-05T08:00:00Z"))
            .build();
    given(service.getHistory(gradeId)).willReturn(List.of(history));

    mockMvc
        .perform(get("/grades/{gradeId}/history", gradeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].reason").value("typo"))
        .andExpect(jsonPath("$[0].previousValue").value(12.0))
        .andExpect(jsonPath("$[0].newValue").value(14.5));
  }
}
