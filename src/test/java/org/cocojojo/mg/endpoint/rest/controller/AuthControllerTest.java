package org.cocojojo.mg.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.UserResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.UnauthorizedException;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService service;
  @MockBean private JwtService jwtService;

  @Test
  void login_returns_token_and_user() throws Exception {
    var user =
        UserResponse.builder()
            .id(UUID.randomUUID())
            .firstname("Ada")
            .lastname("Lovelace")
            .email("ada@hei.school")
            .role(Role.ADMIN)
            .build();
    var response = AuthResponse.builder().token("jwt-token").user(user).build();
    given(service.login(any(LoginRequest.class))).willReturn(response);

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ada@hei.school\",\"password\":\"secret\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt-token"))
        .andExpect(jsonPath("$.user.email").value("ada@hei.school"))
        .andExpect(jsonPath("$.user.role").value("ADMIN"));
  }

  @Test
  void login_returns_bad_request_when_email_is_blank() throws Exception {
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"password\":\"secret\"}"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).login(any(LoginRequest.class));
  }

  @Test
  void login_returns_bad_request_when_email_is_not_an_email() throws Exception {
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nope\",\"password\":\"secret\"}"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).login(any(LoginRequest.class));
  }

  @Test
  void login_returns_bad_request_when_password_is_missing() throws Exception {
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ada@hei.school\"}"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).login(any(LoginRequest.class));
  }

  @Test
  void login_returns_unauthorized_when_credentials_are_invalid() throws Exception {
    given(service.login(any(LoginRequest.class)))
        .willThrow(new UnauthorizedException("Invalid credentials"));

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ada@hei.school\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Invalid credentials"))
        .andExpect(jsonPath("$.status").value("401 UNAUTHORIZED"));
  }
}
