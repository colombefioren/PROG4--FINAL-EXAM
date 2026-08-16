package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.TeacherRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.TeacherResponse;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class TeacherIT extends FacadeIT {

  @Autowired private AdminRepository adminRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @LocalServerPort int port;
  private WebTestClient webTestClient;
  private String adminToken;
  private String adminEmail;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    adminEmail = "teacher-admin-" + UUID.randomUUID().toString().substring(0, 8) + "@hei.school";
    saveAdmin(adminEmail, "secret123", false);
  }

  private void saveAdmin(String email, String rawPassword, boolean isDeleted) {
    adminRepository.save(
        JAdmin.builder()
            .firstname("Ada")
            .lastname("Lovelace")
            .email(email)
            .password(passwordEncoder.encode(rawPassword))
            .isDeleted(isDeleted)
            .build());
  }

  private String adminToken() {
    if (adminToken == null) {
      adminToken =
          webTestClient
              .post()
              .uri("/auth/login")
              .bodyValue(new LoginRequest(adminEmail, "secret123"))
              .exchange()
              .expectStatus()
              .isOk()
              .expectBody(AuthResponse.class)
              .returnResult()
              .getResponseBody()
              .token();
    }
    return adminToken;
  }

  private TeacherResponse createTeacher(String email) {
    return webTestClient
        .put()
        .uri("/teachers")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            TeacherRequest.builder()
                .firstname("John")
                .lastname("Doe")
                .email(email)
                .password("password123")
                .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(TeacherResponse.class)
        .returnResult()
        .getResponseBody();
  }

  @Test
  void adminCanCreateATeacher() {
    var teacher = createTeacher(uniqueEmail());

    assertNotNull(teacher.id());
    assertEquals("John", teacher.firstname());
    assertEquals("Doe", teacher.lastname());
  }

  @Test
  void adminCanUpdateAnExistingTeacher() {
    var teacher = createTeacher(uniqueEmail());

    var updated =
        webTestClient
            .put()
            .uri("/teachers")
            .header("Authorization", "Bearer " + adminToken())
            .bodyValue(
                TeacherRequest.builder()
                    .id(teacher.id())
                    .firstname("Jane")
                    .lastname("Smith")
                    .email(teacher.email())
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(TeacherResponse.class)
            .returnResult()
            .getResponseBody();

    assertEquals(teacher.id(), updated.id());
    assertEquals("Jane", updated.firstname());
    assertEquals("Smith", updated.lastname());
  }

  @Test
  void creatingATeacherWithoutPasswordIsRejected() {
    webTestClient
        .put()
        .uri("/teachers")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            TeacherRequest.builder().firstname("John").lastname("Doe").email(uniqueEmail()).build())
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void adminCanListTeachers() {
    createTeacher(uniqueEmail());
    createTeacher(uniqueEmail());

    var teachers =
        webTestClient
            .get()
            .uri("/teachers")
            .header("Authorization", "Bearer " + adminToken())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(TeacherResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(teachers);
    assertEquals(2, teachers.size());
  }

  @Test
  void adminCanReadATeacherById() {
    var teacher = createTeacher(uniqueEmail());

    var fetched =
        webTestClient
            .get()
            .uri("/teachers/" + teacher.id())
            .header("Authorization", "Bearer " + adminToken())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(TeacherResponse.class)
            .returnResult()
            .getResponseBody();

    assertEquals(teacher.id(), fetched.id());
    assertEquals(teacher.email(), fetched.email());
  }

  @Test
  void readingAnUnknownTeacherReturnsNotFound() {
    webTestClient
        .get()
        .uri("/teachers/" + UUID.randomUUID())
        .header("Authorization", "Bearer " + adminToken())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void unauthenticatedPutIsRejected() {
    webTestClient
        .put()
        .uri("/teachers")
        .bodyValue(
            TeacherRequest.builder()
                .firstname("John")
                .lastname("Doe")
                .email(uniqueEmail())
                .password("password123")
                .build())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void teacherCanReadTheirOwnProfile() {
    var email = uniqueEmail();
    var teacher = createTeacher(email);

    var token =
        webTestClient
            .post()
            .uri("/auth/login")
            .bodyValue(new LoginRequest(email, "password123"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody()
            .token();

    var fetched =
        webTestClient
            .get()
            .uri("/teachers/" + teacher.id())
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(TeacherResponse.class)
            .returnResult()
            .getResponseBody();

    assertEquals(teacher.id(), fetched.id());
  }

  @Test
  void teacherCannotCreateAUser() {
    var email = uniqueEmail();
    createTeacher(email);

    var token =
        webTestClient
            .post()
            .uri("/auth/login")
            .bodyValue(new LoginRequest(email, "password123"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody()
            .token();

    webTestClient
        .put()
        .uri("/teachers")
        .header("Authorization", "Bearer " + token)
        .bodyValue(
            TeacherRequest.builder()
                .firstname("John")
                .lastname("Doe")
                .email(uniqueEmail())
                .password("password123")
                .build())
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  private String uniqueEmail() {
    return "teacher-" + UUID.randomUUID().toString().substring(0, 8) + "@hei.school";
  }
}
