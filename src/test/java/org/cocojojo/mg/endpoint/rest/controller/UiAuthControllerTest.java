package org.cocojojo.mg.endpoint.rest.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.UiLoginResult;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UiAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class UiAuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService service;
  @MockBean private JwtService jwtService;

  @Test
  void login_page_returns_login_view() throws Exception {
    mockMvc.perform(get("/ui/login")).andExpect(status().isOk()).andExpect(view().name("login"));
  }

  @Test
  void forbidden_page_returns_forbidden_view() throws Exception {
    mockMvc
        .perform(get("/ui/forbidden"))
        .andExpect(status().isOk())
        .andExpect(view().name("forbidden"));
  }

  @Test
  void login_redirects_admin_to_promotions_and_sets_cookie() throws Exception {
    given(service.loginForUi(any(LoginRequest.class)))
        .willReturn(
            UiLoginResult.builder().token("jwt-token").redirectUrl("/ui/promotions").build());

    mockMvc
        .perform(post("/ui/login").param("email", "ada@hei.school").param("password", "secret"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ui/promotions"))
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("hei_token=jwt-token")));
  }

  @Test
  void login_redirects_non_admin_to_forbidden() throws Exception {
    given(service.loginForUi(any(LoginRequest.class)))
        .willReturn(
            UiLoginResult.builder().token("jwt-token").redirectUrl("/ui/forbidden").build());

    mockMvc
        .perform(post("/ui/login").param("email", "ada@hei.school").param("password", "secret"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ui/forbidden"));
  }

  @Test
  void login_returns_login_view_with_error_on_failure() throws Exception {
    given(service.loginForUi(any(LoginRequest.class)))
        .willThrow(new IllegalArgumentException("Invalid credentials"));

    mockMvc
        .perform(post("/ui/login").param("email", "ada@hei.school").param("password", "wrong"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(model().attribute("error", "Invalid credentials"));
  }

  @Test
  void logout_clears_cookie_and_redirects_to_login() throws Exception {
    mockMvc
        .perform(post("/ui/logout"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/ui/login"))
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("hei_token=")))
        .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
  }
}
