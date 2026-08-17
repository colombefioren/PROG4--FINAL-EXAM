package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Objects;
import java.util.UUID;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.AdminRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.AdminResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.repository.model.JTeacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class AdminIT extends FacadeIT {

  @Autowired private AdminRepository adminRepository;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @LocalServerPort int port;
  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    adminRepository.deleteAll();
    teacherRepository.deleteAll();
  }

  private String loginToken(String email, String rawPassword) {
    return Objects.requireNonNull(
            webTestClient
                .post()
                .uri("/auth/login")
                .bodyValue(new LoginRequest(email, rawPassword))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody())
        .token();
  }

  private JAdmin saveAdmin(String email, String rawPassword) {
    return adminRepository.save(
        JAdmin.builder()
            .firstname("Ada")
            .lastname("Lovelace")
            .email(email)
            .password(passwordEncoder.encode(rawPassword))
            .build());
  }

  private AdminRequest adminRequest(String firstname, String lastname, String email) {
    return AdminRequest.builder().firstname(firstname).lastname(lastname).email(email).build();
  }

  @Test
  void adminCanUpdateOwnProfile() {
    var admin = saveAdmin("admin-" + UUID.randomUUID() + "@hei.school", "secret123");
    var token = loginToken(admin.getEmail(), "secret123");

    var updated =
        webTestClient
            .put()
            .uri("/admins/" + admin.getId())
            .header("Authorization", "Bearer " + token)
            .bodyValue(
                adminRequest("Grace", "Hopper", "grace." + UUID.randomUUID() + "@hei.school"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AdminResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(updated);
    assertEquals(admin.getId(), updated.id());
    assertEquals("Grace", updated.firstname());
    assertEquals("Hopper", updated.lastname());
  }

  @Test
  void adminCannotUpdateAnotherAdmin() {
    var self = saveAdmin("self-" + UUID.randomUUID() + "@hei.school", "secret123");
    var other = saveAdmin("other-" + UUID.randomUUID() + "@hei.school", "secret123");
    var token = loginToken(self.getEmail(), "secret123");

    webTestClient
        .put()
        .uri("/admins/" + other.getId())
        .header("Authorization", "Bearer " + token)
        .bodyValue(adminRequest("Grace", "Hopper", "grace." + UUID.randomUUID() + "@hei.school"))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void teacherCannotUpdateAdmin() {
    var admin = saveAdmin("admin-" + UUID.randomUUID() + "@hei.school", "secret123");
    var teacher =
        teacherRepository.save(
            JTeacher.builder()
                .firstname("Alan")
                .lastname("Turing")
                .email("teacher-" + UUID.randomUUID() + "@hei.school")
                .password(passwordEncoder.encode("secret123"))
                .build());

    webTestClient
        .put()
        .uri("/admins/" + admin.getId())
        .header("Authorization", "Bearer " + loginToken(teacher.getEmail(), "secret123"))
        .bodyValue(adminRequest("Grace", "Hopper", "grace." + UUID.randomUUID() + "@hei.school"))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void adminCanUpdateOwnPassword() {
    var admin = saveAdmin("admin-" + UUID.randomUUID() + "@hei.school", "secret123");
    var token = loginToken(admin.getEmail(), "secret123");
    var newPassword = "renewed-" + UUID.randomUUID();

    webTestClient
        .put()
        .uri("/admins/" + admin.getId())
        .header("Authorization", "Bearer " + token)
        .bodyValue(
            AdminRequest.builder()
                .firstname("Ada")
                .lastname("Lovelace")
                .email(admin.getEmail())
                .password(newPassword)
                .build())
        .exchange()
        .expectStatus()
        .isOk();

    webTestClient
        .post()
        .uri("/auth/login")
        .bodyValue(new LoginRequest(admin.getEmail(), newPassword))
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void unauthenticatedCannotUpdateAdmin() {
    webTestClient
        .put()
        .uri("/admins/" + UUID.randomUUID())
        .bodyValue(adminRequest("Grace", "Hopper", "grace@hei.school"))
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void adminCanGetOwnProfile() {
    var admin = saveAdmin("admin-" + UUID.randomUUID() + "@hei.school", "secret123");
    var token = loginToken(admin.getEmail(), "secret123");

    var fetched =
        webTestClient
            .get()
            .uri("/admins/" + admin.getId())
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AdminResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(fetched);
    assertEquals(admin.getId(), fetched.id());
    assertEquals("Ada", fetched.firstname());
    assertEquals("Lovelace", fetched.lastname());
    assertEquals(admin.getEmail(), fetched.email());
  }

  @Test
  void adminCannotGetAnotherAdmin() {
    var self = saveAdmin("self-" + UUID.randomUUID() + "@hei.school", "secret123");
    var other = saveAdmin("other-" + UUID.randomUUID() + "@hei.school", "secret123");
    var token = loginToken(self.getEmail(), "secret123");

    webTestClient
        .get()
        .uri("/admins/" + other.getId())
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void teacherCannotGetAdmin() {
    var admin = saveAdmin("admin-" + UUID.randomUUID() + "@hei.school", "secret123");
    var teacher =
        teacherRepository.save(
            JTeacher.builder()
                .firstname("Alan")
                .lastname("Turing")
                .email("teacher-" + UUID.randomUUID() + "@hei.school")
                .password(passwordEncoder.encode("secret123"))
                .build());

    webTestClient
        .get()
        .uri("/admins/" + admin.getId())
        .header("Authorization", "Bearer " + loginToken(teacher.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void unauthenticatedCannotGetAdmin() {
    webTestClient
        .get()
        .uri("/admins/" + UUID.randomUUID())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }
}
