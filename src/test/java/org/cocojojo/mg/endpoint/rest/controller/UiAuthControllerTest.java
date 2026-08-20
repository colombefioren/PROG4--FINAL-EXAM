package org.cocojojo.mg.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UiAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class UiAuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService authService;
  @MockBean private JwtService jwtService;

  @Test
  void login_page_returns_view() throws Exception {
    mockMvc.perform(get("/ui/login")).andExpect(status().isOk()).andExpect(view().name("login"));
  }

  @Test
  void forbidden_page_returns_view() throws Exception {
    mockMvc
        .perform(get("/ui/forbidden"))
        .andExpect(status().isOk())
        .andExpect(view().name("forbidden"));
  }

  @Test
  void login_redirects_admin_to_promotions_with_cookie() throws Exception {
    given(authService.loginForUi(any(LoginRequest.class)))
        .willReturn(
            UiLoginResult.builder().token("jwt-token").redirectUrl("/ui/promotions").build());

    mockMvc
        .perform(post("/ui/login").param("email", "admin@hei.school").param("password", "secret"))
        .andExpect(status().is3xxRedirection())
        .andExpect(view().name("redirect:/ui/promotions"))
        .andExpect(
            header()
                .string("Set-Cookie", org.hamcrest.Matchers.containsString("hei_token=jwt-token")));
  }

  @Test
  void login_renders_error_on_bad_credentials() throws Exception {
    given(authService.loginForUi(any(LoginRequest.class)))
        .willThrow(
            new org.cocojojo.mg.endpoint.rest.controller.exception.UnauthorizedException(
                "Invalid credentials"));

    mockMvc
        .perform(post("/ui/login").param("email", "nope@hei.school").param("password", "wrong"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"))
        .andExpect(model().attribute("error", "Invalid credentials"));
  }

  @Test
  void logout_redirects_and_expires_cookie() throws Exception {
    mockMvc
        .perform(post("/ui/logout"))
        .andExpect(status().is3xxRedirection())
        .andExpect(view().name("redirect:/ui/login"))
        .andExpect(
            header().string("Set-Cookie", org.hamcrest.Matchers.containsString("hei_token=")));
  }
}
