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

import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.TeacherResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ConflictException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.service.TeacherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TeacherController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private TeacherService service;
  @MockBean private JwtService jwtService;

  private final UUID id = UUID.randomUUID();

  @Test
  void get_all_returns_page() throws Exception {
    given(service.getAll(any(Pageable.class)))
        .willReturn(new PageImpl<>(java.util.List.of(teacherResponse())));

    mockMvc
        .perform(get("/teachers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(id.toString()))
        .andExpect(jsonPath("$.content[0].email").value("grace@hei.school"));
  }

  @Test
  void get_returns_teacher_when_found() throws Exception {
    given(service.getById(id)).willReturn(teacherResponse());

    mockMvc
        .perform(get("/teachers/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstname").value("Grace"))
        .andExpect(jsonPath("$.email").value("grace@hei.school"));
  }

  @Test
  void update_returns_updated_teacher() throws Exception {
    given(service.upsert(any())).willReturn(teacherResponse());

    mockMvc
        .perform(
            put("/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"firstname\":\"Grace\",\"lastname\":\"Hopper\",\"email\":\"grace@hei.school\",\"password\":\"secret\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("grace@hei.school"));
  }

  @Test
  void update_returns_bad_request_when_email_is_malformed() throws Exception {
    mockMvc
        .perform(
            put("/teachers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstname\":\"Grace\",\"lastname\":\"Hopper\",\"email\":\"nope\"}"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).upsert(any());
  }

  @Test
  void delete_returns_no_content() throws Exception {
    mockMvc.perform(delete("/teachers/{id}", id)).andExpect(status().isNoContent());

    then(service).should().delete(id);
  }

  @Test
  void delete_propagates_forbidden() throws Exception {
    org.mockito.BDDMockito.willThrow(new ForbiddenAccessException("Not allowed"))
        .given(service)
        .delete(id);

    mockMvc
        .perform(delete("/teachers/{id}", id))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("Not allowed"));
  }

  @Test
  void delete_propagates_conflict() throws Exception {
    org.mockito.BDDMockito.willThrow(
            new ConflictException(
                "Teacher with id: " + id + " has recorded grade changes and cannot be deleted."))
        .given(service)
        .delete(id);

    mockMvc
        .perform(delete("/teachers/{id}", id))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.error")
                .value(
                    "Teacher with id: "
                        + id
                        + " has recorded grade changes and cannot be deleted."));
  }

  private TeacherResponse teacherResponse() {
    return TeacherResponse.builder()
        .id(id)
        .firstname("Grace")
        .lastname("Hopper")
        .email("grace@hei.school")
        .build();
  }
}
