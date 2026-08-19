package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.security.JwtAuthenticationFilter;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.repository.model.JTeacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

class LoginPageIT extends FacadeIT {

  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  @Autowired private AdminRepository adminRepository;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @LocalServerPort int port;
  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    teacherRepository.deleteAll();
    adminRepository.deleteAll();
  }

  private String unique(String prefix) {
    return "login-" + prefix + SEQUENCE.incrementAndGet() + "@hei.school";
  }

  private JAdmin saveAdmin() {
    return adminRepository.save(
        JAdmin.builder()
            .firstname("Ada")
            .lastname("Lovelace")
            .email(unique("admin"))
            .password(passwordEncoder.encode("secret123"))
            .build());
  }

  private JTeacher saveTeacher() {
    return teacherRepository.save(
        JTeacher.builder()
            .firstname("Grace")
            .lastname("Hopper")
            .email(unique("teacher"))
            .password(passwordEncoder.encode("secret123"))
            .build());
  }

  @Test
  void loginPageIsPubliclyAccessible() {
    var admin = saveAdmin();

    webTestClient
        .get()
        .uri("/ui/login")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .consumeWith(
            body -> {
              var html = new String(body.getResponseBody());
              assertTrue(html.contains("Sign in"));
              assertTrue(html.contains("name=\"email\""));
              assertTrue(html.contains("name=\"password\""));
            });

    // The page is reachable without any credential, even with an account present.
    assertNotNull(admin);
  }

  @Test
  void loginWithValidCredentialsSetsCookieAndRedirects() {
    var admin = saveAdmin();

    var result =
        webTestClient
            .post()
            .uri("/ui/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters.fromFormData("email", admin.getEmail()).with("password", "secret123"))
            .exchange()
            .expectStatus()
            .is3xxRedirection()
            .returnResult(Void.class);

    assertTrue(
        result.getResponseHeaders().getFirst(HttpHeaders.LOCATION).endsWith("/ui/promotions"));
    var setCookie = result.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertNotNull(setCookie);
    assertTrue(setCookie.startsWith(JwtAuthenticationFilter.TOKEN_COOKIE + "="));
    assertTrue(setCookie.contains("HttpOnly"));
  }

  @Test
  void teacherLoginRedirectsToForbiddenPage() {
    var teacher = saveTeacher();

    var result =
        webTestClient
            .post()
            .uri("/ui/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters.fromFormData("email", teacher.getEmail())
                    .with("password", "secret123"))
            .exchange()
            .expectStatus()
            .is3xxRedirection()
            .returnResult(Void.class);

    assertTrue(
        result.getResponseHeaders().getFirst(HttpHeaders.LOCATION).endsWith("/ui/forbidden"));
    var setCookie = result.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertNotNull(setCookie);
    assertTrue(setCookie.startsWith(JwtAuthenticationFilter.TOKEN_COOKIE + "="));
  }

  @Test
  void forbiddenPageIsReachableWithoutAdminRole() {
    var teacher = saveTeacher();
    var cookie = loginAndGetCookie(teacher.getEmail(), "secret123", "/ui/forbidden");

    webTestClient
        .get()
        .uri("/ui/forbidden")
        .cookie(JwtAuthenticationFilter.TOKEN_COOKIE, cookie)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .consumeWith(
            body -> {
              var html = new String(body.getResponseBody());
              assertTrue(html.contains("Forbidden"));
              assertTrue(html.contains("FORBIDDEN ACCESS"));
            });
  }

  @Test
  void loginWithWrongPasswordRendersError() {
    var admin = saveAdmin();

    var result =
        webTestClient
            .post()
            .uri("/ui/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("email", admin.getEmail()).with("password", "wrong"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult();

    var html = new String(result.getResponseBody());
    assertTrue(html.contains("Invalid credentials"));
    assertTrue(html.contains("Sign in"));
    assertTrue(html.contains("name=\"email\""));
  }

  @Test
  void loginWithUnknownEmailRendersError() {
    var result =
        webTestClient
            .post()
            .uri("/ui/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("email", "nobody@hei.school").with("password", "x"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult();

    assertTrue(new String(result.getResponseBody()).contains("Invalid credentials"));
  }

  @Test
  void loginWithDeletedAccountRendersError() {
    var email = unique("deleted");
    adminRepository.save(
        JAdmin.builder()
            .firstname("Ada")
            .lastname("Lovelace")
            .email(email)
            .password(passwordEncoder.encode("secret123"))
            .isDeleted(true)
            .build());

    var result =
        webTestClient
            .post()
            .uri("/ui/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("email", email).with("password", "secret123"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult();

    assertTrue(new String(result.getResponseBody()).contains("This account has been disabled"));
  }

  @Test
  void cookieGrantsAccessToUiPages() {
    var admin = saveAdmin();
    var cookie = loginAndGetCookie(admin.getEmail(), "secret123", "/ui/promotions");

    webTestClient
        .get()
        .uri("/ui/promotions")
        .cookie(JwtAuthenticationFilter.TOKEN_COOKIE, cookie)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .consumeWith(body -> assertTrue(new String(body.getResponseBody()).contains("Promotions")));
  }

  @Test
  void logoutClearsCookieAndRevokesAccess() {
    var admin = saveAdmin();
    var cookie = loginAndGetCookie(admin.getEmail(), "secret123", "/ui/promotions");

    var result =
        webTestClient
            .post()
            .uri("/ui/logout")
            .cookie(JwtAuthenticationFilter.TOKEN_COOKIE, cookie)
            .exchange()
            .expectStatus()
            .is3xxRedirection()
            .returnResult(Void.class);

    assertTrue(result.getResponseHeaders().getFirst(HttpHeaders.LOCATION).endsWith("/ui/login"));
    var setCookie = result.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertNotNull(setCookie);
    assertTrue(setCookie.contains("Max-Age=0"));

    webTestClient
        .get()
        .uri("/ui/promotions")
        .cookie(JwtAuthenticationFilter.TOKEN_COOKIE, "")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void teacherCannotReachAdminUiAfterLogin() {
    var teacher = saveTeacher();
    var cookie = loginAndGetCookie(teacher.getEmail(), "secret123", "/ui/forbidden");

    webTestClient
        .get()
        .uri("/ui/promotions")
        .cookie(JwtAuthenticationFilter.TOKEN_COOKIE, cookie)
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  private String loginAndGetCookie(String email, String password, String redirectPath) {
    var result =
        webTestClient
            .post()
            .uri("/ui/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("email", email).with("password", password))
            .exchange()
            .expectStatus()
            .is3xxRedirection()
            .returnResult(Void.class);
    assertTrue(result.getResponseHeaders().getFirst(HttpHeaders.LOCATION).endsWith(redirectPath));
    var setCookie = result.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertNotNull(setCookie);
    assertTrue(setCookie.startsWith(JwtAuthenticationFilter.TOKEN_COOKIE + "="));
    return extractCookieValue(setCookie);
  }

  private String extractCookieValue(String setCookie) {
    var prefix = JwtAuthenticationFilter.TOKEN_COOKIE + "=";
    var value = setCookie.substring(setCookie.indexOf(prefix) + prefix.length());
    int end = value.indexOf(';');
    return end >= 0 ? value.substring(0, end) : value;
  }
}
