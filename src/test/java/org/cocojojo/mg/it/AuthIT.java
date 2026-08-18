package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class AuthIT extends FacadeIT {

  @Autowired private AdminRepository adminRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @LocalServerPort int port;
  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    adminRepository.deleteAll();
  }

  private JAdmin saveAdmin(String email, String rawPassword, boolean isDeleted) {
    return adminRepository.save(
        JAdmin.builder()
            .firstname("Ada")
            .lastname("Lovelace")
            .email(email)
            .password(passwordEncoder.encode(rawPassword))
            .isDeleted(isDeleted)
            .build());
  }

  @Test
  void pingShouldBePublic() {
    webTestClient.get().uri("/ping").exchange().expectStatus().isOk();
  }

  @Test
  void shouldLoginWithValidCredentials() {
    saveAdmin("admin@hei.school", "secret123", false);

    var response =
        webTestClient
            .post()
            .uri("/auth/login")
            .bodyValue(new LoginRequest("admin@hei.school", "secret123"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(response);
    assertNotNull(response.token());
    assertEquals("admin@hei.school", response.user().email());
    assertEquals(Role.ADMIN, response.user().role());
  }

  @Test
  void shouldRejectWrongPassword() {
    saveAdmin("admin2@hei.school", "correct", false);

    webTestClient
        .post()
        .uri("/auth/login")
        .bodyValue(new LoginRequest("admin2@hei.school", "wrong"))
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void shouldRejectUnknownEmail() {
    webTestClient
        .post()
        .uri("/auth/login")
        .bodyValue(new LoginRequest("nobody@hei.school", "whatever"))
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody()
        .jsonPath("$.error")
        .isEqualTo("Invalid credentials");
  }

  @Test
  void shouldRejectDeletedAccount() {
    saveAdmin("deleted@hei.school", "secret123", true);

    webTestClient
        .post()
        .uri("/auth/login")
        .bodyValue(new LoginRequest("deleted@hei.school", "secret123"))
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void shouldRejectRequestsWithoutToken() {
    webTestClient.get().uri("/courses").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void shouldRejectInvalidToken() {
    webTestClient
        .get()
        .uri("/courses")
        .header("Authorization", "Bearer invalid-token")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void unknownEndpointReturnsNotFoundInsteadOfUnauthorized() {
    webTestClient.get().uri("/definitely-not-an-endpoint").exchange().expectStatus().isNotFound();
  }

  @Test
  void protectedEndpointStillChallengesWithBasicAuth() {
    webTestClient
        .get()
        .uri("/courses")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .valueEquals("WWW-Authenticate", "Basic realm=\"hei\"");
  }
}
