package org.cocojojo.mg.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private CourseService service;
  @MockBean private JwtService jwtService;

  private final UUID id = UUID.randomUUID();

  @Test
  void get_returns_course_when_found() throws Exception {
    given(service.getById(id)).willReturn(courseResponse());

    mockMvc
        .perform(get("/courses/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.code").value("TST1"))
        .andExpect(jsonPath("$.name").value("Test Course"))
        .andExpect(jsonPath("$.credits").value(6))
        .andExpect(jsonPath("$.studentLevel").value("L1"));
  }

  @Test
  void get_returns_not_found_when_missing() throws Exception {
    given(service.getById(id))
        .willThrow(new ResourceNotFoundException("Course with id: " + id + " not found."));

    mockMvc
        .perform(get("/courses/{id}", id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Course with id: " + id + " not found."));
  }

  @Test
  void update_returns_updated_course() throws Exception {
    given(service.upsert(any())).willReturn(courseResponse());

    mockMvc
        .perform(
            put("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"code\":\"TST1\",\"name\":\"Test"
                        + " Course\",\"credits\":6,\"totalHours\":20,\"studentLevel\":\"L1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("TST1"));
  }

  @Test
  void update_returns_bad_request_when_body_is_invalid() throws Exception {
    mockMvc
        .perform(
            put("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"\",\"name\":\"\",\"credits\":0,\"studentLevel\":null}"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).upsert(any());
  }

  @Test
  void update_propagates_forbidden_from_service() throws Exception {
    given(service.upsert(any())).willThrow(new ForbiddenAccessException("Not allowed"));

    mockMvc
        .perform(
            put("/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"code\":\"TST1\",\"name\":\"Test"
                        + " Course\",\"credits\":6,\"totalHours\":20,\"studentLevel\":\"L1\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("Not allowed"));
  }

  private CourseResponse courseResponse() {
    return CourseResponse.builder()
        .id(id)
        .code("TST1")
        .name("Test Course")
        .credits(6)
        .totalHours(20)
        .studentLevel(StudentLevel.L1)
        .build();
  }
}
