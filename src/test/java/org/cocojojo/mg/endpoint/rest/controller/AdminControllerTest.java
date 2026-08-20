package org.cocojojo.mg.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.AdminRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.AdminResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AdminService service;
  @MockBean private JwtService jwtService;

  private final UUID id = UUID.randomUUID();
  private final AdminResponse response =
      AdminResponse.builder()
          .id(id)
          .firstname("Ada")
          .lastname("Lovelace")
          .email("a@hei.school")
          .build();

  @Test
  void get_returns_admin_when_found() throws Exception {
    given(service.getById(id)).willReturn(response);

    mockMvc
        .perform(get("/admins/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.firstname").value("Ada"))
        .andExpect(jsonPath("$.lastname").value("Lovelace"))
        .andExpect(jsonPath("$.email").value("a@hei.school"));
  }

  @Test
  void get_returns_not_found_when_missing() throws Exception {
    given(service.getById(id))
        .willThrow(new ResourceNotFoundException("Admin with id:" + id + " not found."));

    mockMvc
        .perform(get("/admins/{id}", id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Admin with id:" + id + " not found."))
        .andExpect(jsonPath("$.status").value("404 NOT_FOUND"));
  }

  @Test
  void update_returns_updated_admin() throws Exception {
    var request =
        AdminRequest.builder().firstname("Grace").lastname("Hopper").email("g@hei.school").build();
    var updated =
        AdminResponse.builder()
            .id(id)
            .firstname("Grace")
            .lastname("Hopper")
            .email("g@hei.school")
            .build();
    given(service.update(eq(id), any(AdminRequest.class))).willReturn(updated);

    mockMvc
        .perform(
            put("/admins/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"firstname\":\"Grace\",\"lastname\":\"Hopper\",\"email\":\"g@hei.school\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstname").value("Grace"))
        .andExpect(jsonPath("$.email").value("g@hei.school"));
  }

  @Test
  void update_returns_bad_request_when_body_is_invalid() throws Exception {
    mockMvc
        .perform(
            put("/admins/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstname\":\"\",\"lastname\":\"\",\"email\":\"not-an-email\"}"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).update(any(UUID.class), any(AdminRequest.class));
  }

  @Test
  void update_returns_bad_request_when_email_malformed() throws Exception {
    mockMvc
        .perform(
            put("/admins/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstname\":\"Ada\",\"lastname\":\"Lovelace\",\"email\":\"nope\"}"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).update(any(UUID.class), any(AdminRequest.class));
  }

  @Test
  void update_propagates_forbidden_from_service() throws Exception {
    given(service.update(eq(id), any(AdminRequest.class)))
        .willThrow(new ForbiddenAccessException("Not allowed"));

    mockMvc
        .perform(
            put("/admins/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"firstname\":\"Ada\",\"lastname\":\"Lovelace\",\"email\":\"a@hei.school\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("Not allowed"));
  }
}
