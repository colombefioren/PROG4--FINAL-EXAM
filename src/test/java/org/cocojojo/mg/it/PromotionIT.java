package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionResponse;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class PromotionIT extends FacadeIT {

  @Autowired private AdminRepository adminRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @LocalServerPort int port;
  private WebTestClient webTestClient;
  private String adminToken;
  private String adminEmail;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    adminEmail = "admin-" + uniqueRef().toLowerCase() + "@hei.school";
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

  WebTestClient webTestClient() {
    return webTestClient;
  }

  private static String uniqueRef() {
    return "P" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  @Test
  void upsertCreatesAndCanBeFetchedById() {
    var ref = uniqueRef();

    var created =
        webTestClient()
            .put()
            .uri("/promotions")
            .header("Authorization", "Bearer " + adminToken())
            .bodyValue(
                PromotionRequest.builder()
                    .ref(ref)
                    .name("Promotion " + ref)
                    .entryYear(2024)
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(PromotionResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(created.id());
    assertEquals(ref, created.ref());

    var fetched =
        webTestClient()
            .get()
            .uri("/promotions/{id}", created.id())
            .header("Authorization", "Bearer " + adminToken())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(PromotionResponse.class)
            .returnResult()
            .getResponseBody();

    assertEquals(created.id(), fetched.id());
    assertEquals(ref, fetched.ref());
    assertEquals(2024, fetched.entryYear());
  }

  @Test
  void upsertWithExistingIdUpdatesSameRow() {
    var ref = uniqueRef();
    var created =
        webTestClient()
            .put()
            .uri("/promotions")
            .header("Authorization", "Bearer " + adminToken())
            .bodyValue(
                PromotionRequest.builder().ref(ref).name("Original name").entryYear(2024).build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(PromotionResponse.class)
            .returnResult()
            .getResponseBody();

    var updated =
        webTestClient()
            .put()
            .uri("/promotions")
            .header("Authorization", "Bearer " + adminToken())
            .bodyValue(
                PromotionRequest.builder()
                    .id(created.id())
                    .ref(ref)
                    .name("Renamed promotion")
                    .entryYear(2025)
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(PromotionResponse.class)
            .returnResult()
            .getResponseBody();

    assertEquals(created.id(), updated.id());
    assertEquals("Renamed promotion", updated.name());
    assertEquals(2025, updated.entryYear());

    var all =
        webTestClient()
            .get()
            .uri("/promotions")
            .header("Authorization", "Bearer " + adminToken())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new ParameterizedTypeReference<TestPage<PromotionResponse>>() {})
            .returnResult()
            .getResponseBody()
            .content()
            .stream()
            .filter(p -> p.id().equals(created.id()))
            .toList();

    assertEquals(1, all.size());
    assertEquals("Renamed promotion", all.get(0).name());
  }

  @Test
  void upsertWithoutRequiredFieldsIsRejected() {
    webTestClient()
        .put()
        .uri("/promotions")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(PromotionRequest.builder().build())
        .exchange()
        .expectStatus()
        .isBadRequest();
  }
}
