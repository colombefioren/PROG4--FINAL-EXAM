package org.cocojojo.mg.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupFlowResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.StudentResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.service.GroupFlowService;
import org.cocojojo.mg.service.ResultService;
import org.cocojojo.mg.service.StudentService;
import org.cocojojo.mg.service.TranscriptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = StudentController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private StudentService studentService;
  @MockBean private GroupFlowService groupFlowService;
  @MockBean private TranscriptService transcriptService;
  @MockBean private ResultService resultService;
  @MockBean private JwtService jwtService;

  private final UUID id = UUID.randomUUID();

  @Test
  void get_all_returns_page() throws Exception {
    var student = studentResponse();
    given(studentService.getAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(student)));

    mockMvc
        .perform(get("/students"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(id.toString()))
        .andExpect(jsonPath("$.content[0].std").value("STD-01"));
  }

  @Test
  void get_returns_student_when_found() throws Exception {
    given(studentService.getById(id)).willReturn(studentResponse());

    mockMvc
        .perform(get("/students/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.std").value("STD-01"))
        .andExpect(jsonPath("$.email").value("alan@hei.school"));
  }

  @Test
  void update_returns_updated_student() throws Exception {
    given(studentService.upsert(any())).willReturn(studentResponse());

    mockMvc
        .perform(
            put("/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"firstname\":\"Alan\",\"lastname\":\"Turing\",\"email\":\"alan@hei.school\",\"password\":\"secret\",\"groupId\":\""
                        + UUID.randomUUID()
                        + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.std").value("STD-01"));
  }

  @Test
  void update_returns_bad_request_when_email_is_malformed() throws Exception {
    mockMvc
        .perform(
            put("/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstname\":\"Alan\",\"lastname\":\"Turing\",\"email\":\"nope\"}"))
        .andExpect(status().isBadRequest());

    then(studentService).should(never()).upsert(any());
  }

  @Test
  void get_group_flows_requires_admin_or_self() throws Exception {
    var flow =
        GroupFlowResponse.builder()
            .id(UUID.randomUUID())
            .studentId(id)
            .groupId(UUID.randomUUID())
            .groupFlowType(GroupFlowType.JOIN)
            .build();
    given(groupFlowService.getHistory(id)).willReturn(List.of(flow));

    mockMvc
        .perform(get("/students/{id}/group-flows", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].studentId").value(id.toString()))
        .andExpect(jsonPath("$[0].groupFlowType").value("JOIN"));
  }

  @Test
  void get_group_flows_propagates_forbidden() throws Exception {
    org.mockito.BDDMockito.willThrow(new ForbiddenAccessException("Not allowed"))
        .given(studentService)
        .assertAdminOrSelf(id);

    mockMvc
        .perform(get("/students/{id}/group-flows", id))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("Not allowed"));
  }

  @Test
  void request_transcript_returns_accepted() throws Exception {
    mockMvc
        .perform(post("/students/{id}/yearly-results/{level}/transcript", id, "L3"))
        .andExpect(status().isAccepted());

    then(transcriptService).should().requestTranscript(eq(id), eq(StudentLevel.L3));
  }

  @Test
  void move_to_group_returns_flow() throws Exception {
    var flow =
        GroupFlowResponse.builder()
            .id(UUID.randomUUID())
            .studentId(id)
            .groupId(UUID.randomUUID())
            .groupFlowType(GroupFlowType.JOIN)
            .build();
    var jStudent = org.cocojojo.mg.repository.model.JStudent.builder().id(id).build();
    given(studentService.getEntityOrThrow(id)).willReturn(jStudent);
    given(groupFlowService.move(eq(jStudent), any())).willReturn(flow);

    mockMvc
        .perform(
            put("/students/{id}/group-flows", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupId\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groupFlowType").value("JOIN"));
  }

  private StudentResponse studentResponse() {
    return StudentResponse.builder()
        .id(id)
        .std("STD-01")
        .firstname("Alan")
        .lastname("Turing")
        .email("alan@hei.school")
        .promotionId(UUID.randomUUID())
        .promotionName("Promotion")
        .build();
  }
}
