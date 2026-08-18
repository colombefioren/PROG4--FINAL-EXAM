package org.cocojojo.mg.endpoint.rest.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.controller.exception.UnauthorizedException;
import org.cocojojo.mg.endpoint.rest.security.JwtAuthenticationFilter;
import org.cocojojo.mg.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class UiAuthController {

  private final AuthService authService;

  @GetMapping("/ui/login")
  public String loginPage() {
    return "login";
  }

  @PostMapping("/ui/login")
  public String login(
      @RequestParam String email,
      @RequestParam String password,
      Model model,
      HttpServletResponse response) {
    try {
      var auth = authService.login(new LoginRequest(email, password));
      response.addHeader(
          HttpHeaders.SET_COOKIE,
          ResponseCookie.from(JwtAuthenticationFilter.TOKEN_COOKIE, auth.token())
              .httpOnly(true)
              .sameSite("Lax")
              .path("/")
              .build()
              .toString());
      return "redirect:/ui/promotions";
    } catch (IllegalArgumentException | IllegalStateException | UnauthorizedException e) {
      model.addAttribute("error", e.getMessage());
      return "login";
    }
  }

  @PostMapping("/ui/logout")
  public String logout(HttpServletResponse response) {
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from(JwtAuthenticationFilter.TOKEN_COOKIE, "")
            .httpOnly(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build()
            .toString());
    return "redirect:/ui/login";
  }
}
