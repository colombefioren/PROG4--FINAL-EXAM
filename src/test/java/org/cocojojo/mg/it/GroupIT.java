package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionResponse;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class GroupIT extends FacadeIT {

  @Autowired private AdminRepository adminRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @LocalServerPort int port;
  private WebTestClient webTestClient;
  private String adminToken;
  private String adminEmail;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    adminEmail = "group-admin-" + UUID.randomUUID().toString().substring(0, 8) + "@hei.school";
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

  private String uniqueRef() {
    return "G" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  private PromotionResponse createPromotion(int entryYear) {
    var name = "Test promotion " + UUID.randomUUID().toString().substring(0, 8);
    return webTestClient
        .put()
        .uri("/promotions")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            PromotionRequest.builder()
                .ref("P" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .name(name)
                .entryYear(entryYear)
                .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(PromotionResponse.class)
        .returnResult()
        .getResponseBody();
  }

  private GroupResponse createGroup(UUID promotionId, Track track) {
    return webTestClient
        .put()
        .uri("/groups")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            GroupRequest.builder().ref(uniqueRef()).promotionId(promotionId).track(track).build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(GroupResponse.class)
        .returnResult()
        .getResponseBody();
  }

  @Test
  void adminCanCreateGroup() {
    var promotion = createPromotion(2024);
    var group = createGroup(promotion.id(), null);

    assertNotNull(group.id());
    assertEquals(promotion.id(), group.promotionId());
    assertEquals(promotion.name(), group.promotionName());
    assertTrue(group.track() == null);
  }

  @Test
  void groupsCanBeFilteredByPromotion() {
    var promotionA = createPromotion(2023);
    var promotionB = createPromotion(2024);
    var groupA = createGroup(promotionA.id(), null);
    createGroup(promotionB.id(), null);

    var groupsOfA =
        webTestClient
            .get()
            .uri("/groups?promotionId=" + promotionA.id())
            .header("Authorization", "Bearer " + adminToken())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(GroupResponse.class)
            .returnResult()
            .getResponseBody();

    assertTrue(groupsOfA.stream().allMatch(g -> promotionA.id().equals(g.promotionId())));
    assertTrue(groupsOfA.stream().anyMatch(g -> groupA.id().equals(g.id())));
  }

  @Test
  void groupTrackIsPreserved() {
    var promotion = createPromotion(2024);
    var elGroup = createGroup(promotion.id(), Track.EL);
    var tnGroup = createGroup(promotion.id(), Track.TN);

    assertEquals(Track.EL, elGroup.track());
    assertEquals(Track.TN, tnGroup.track());
  }

  @Test
  void groupRefIsUppercased() {
    var promotion = createPromotion(2024);
    var lowerRef = "g" + UUID.randomUUID().toString().substring(0, 8);

    var created =
        webTestClient
            .put()
            .uri("/groups")
            .header("Authorization", "Bearer " + adminToken())
            .bodyValue(GroupRequest.builder().ref(lowerRef).promotionId(promotion.id()).build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(GroupResponse.class)
            .returnResult()
            .getResponseBody();

    assertEquals(lowerRef.toUpperCase(), created.ref());
  }

  @Test
  void adminCanUpdateAnExistingGroup() {
    var promotion = createPromotion(2024);
    var created = createGroup(promotion.id(), Track.EL);

    var updated =
        webTestClient
            .put()
            .uri("/groups")
            .header("Authorization", "Bearer " + adminToken())
            .bodyValue(
                GroupRequest.builder()
                    .id(created.id())
                    .ref(created.ref())
                    .promotionId(promotion.id())
                    .track(Track.TN)
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(GroupResponse.class)
            .returnResult()
            .getResponseBody();

    assertEquals(created.id(), updated.id());
    assertEquals(Track.TN, updated.track());
  }

  @Test
  void upsertWithUnknownPromotionIsRejected() {
    webTestClient
        .put()
        .uri("/groups")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(GroupRequest.builder().ref(uniqueRef()).promotionId(UUID.randomUUID()).build())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void upsertWithoutRequiredFieldsIsRejected() {
    webTestClient
        .put()
        .uri("/groups")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(GroupRequest.builder().build())
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void unauthenticatedGetIsRejected() {
    webTestClient.get().uri("/groups").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void unauthenticatedPutIsRejected() {
    webTestClient
        .put()
        .uri("/groups")
        .bodyValue(GroupRequest.builder().ref(uniqueRef()).promotionId(UUID.randomUUID()).build())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }
}
