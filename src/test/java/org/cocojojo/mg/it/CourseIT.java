package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class CourseIT extends FacadeIT {

  @Autowired private AdminRepository adminRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @LocalServerPort int port;
  private WebTestClient webTestClient;
  private String adminToken;
  private String adminEmail;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    adminEmail = "course-admin-" + UUID.randomUUID().toString().substring(0, 8) + "@hei.school";
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

  private String uniqueCode() {
    return "TC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  @Test
  void adminCanCreateCourseAndItAppearsInCatalog() {
    var code = uniqueCode();
    var created =
        webTestClient
            .put()
            .uri("/courses")
            .header("Authorization", "Bearer " + adminToken())
            .bodyValue(
                CourseRequest.builder()
                    .code(code)
                    .name("Test course " + code)
                    .credits(4)
                    .studentLevel(StudentLevel.L1)
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(CourseResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(created.id());
    assertEquals(code, created.code());
    assertEquals(StudentLevel.L1, created.studentLevel());
    assertTrue(created.track() == null);

    var all =
        webTestClient
            .get()
            .uri("/courses")
            .header("Authorization", "Bearer " + adminToken())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(CourseResponse.class)
            .returnResult()
            .getResponseBody();

    assertTrue(all.stream().anyMatch(c -> created.id().equals(c.id())));
    assertTrue(all.stream().anyMatch(c -> "PROG1".equals(c.code())));
  }

  @Test
  void adminCanUpdateAnExistingCourse() {
    var code = uniqueCode();
    var created =
        webTestClient
            .put()
            .uri("/courses")
            .header("Authorization", "Bearer " + adminToken())
            .bodyValue(
                CourseRequest.builder()
                    .code(code)
                    .name("Original course")
                    .credits(4)
                    .studentLevel(StudentLevel.L1)
                    .track(Track.EL)
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(CourseResponse.class)
            .returnResult()
            .getResponseBody();

    var updated =
        webTestClient
            .put()
            .uri("/courses")
            .header("Authorization", "Bearer " + adminToken())
            .bodyValue(
                CourseRequest.builder()
                    .id(created.id())
                    .code(code)
                    .name("Renamed course")
                    .credits(8)
                    .studentLevel(StudentLevel.L2)
                    .track(Track.TN)
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(CourseResponse.class)
            .returnResult()
            .getResponseBody();

    assertEquals(created.id(), updated.id());
    assertEquals("Renamed course", updated.name());
    assertEquals(8, updated.credits());
    assertEquals(StudentLevel.L2, updated.studentLevel());
    assertEquals(Track.TN, updated.track());
  }

  @Test
  void upsertUppercasesTheCode() {
    var lowerCode = "tc" + UUID.randomUUID().toString().substring(0, 8);

    var created =
        webTestClient
            .put()
            .uri("/courses")
            .header("Authorization", "Bearer " + adminToken())
            .bodyValue(
                CourseRequest.builder()
                    .code(lowerCode)
                    .name("Lowercase code course")
                    .credits(3)
                    .studentLevel(StudentLevel.L3)
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(CourseResponse.class)
            .returnResult()
            .getResponseBody();

    assertEquals(lowerCode.toUpperCase(), created.code());
  }

  @Test
  void upsertWithoutRequiredFieldsIsRejected() {
    webTestClient
        .put()
        .uri("/courses")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(CourseRequest.builder().build())
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void unauthenticatedGetIsRejected() {
    webTestClient.get().uri("/courses").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void unauthenticatedPutIsRejected() {
    webTestClient
        .put()
        .uri("/courses")
        .bodyValue(
            CourseRequest.builder()
                .code(uniqueCode())
                .name("x")
                .credits(1)
                .studentLevel(StudentLevel.L1)
                .build())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }
}
