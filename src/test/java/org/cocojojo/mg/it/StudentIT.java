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
import org.cocojojo.mg.endpoint.rest.controller.dto.MoveStudentGroupRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.StudentRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.StudentResponse;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class StudentIT extends FacadeIT {

  @Autowired private AdminRepository adminRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @LocalServerPort int port;
  private WebTestClient webTestClient;
  private String adminToken;
  private String adminEmail;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    adminEmail = "student-admin-" + UUID.randomUUID().toString().substring(0, 8) + "@hei.school";
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

  private PromotionResponse createPromotion(int entryYear) {
    return webTestClient
        .put()
        .uri("/promotions")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            PromotionRequest.builder()
                .ref("P" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .name("Test promotion " + UUID.randomUUID().toString().substring(0, 8))
                .entryYear(entryYear)
                .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(PromotionResponse.class)
        .returnResult()
        .getResponseBody();
  }

  private GroupResponse createGroup(UUID promotionId) {
    return webTestClient
        .put()
        .uri("/groups")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            GroupRequest.builder()
                .ref("G" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .promotionId(promotionId)
                .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(GroupResponse.class)
        .returnResult()
        .getResponseBody();
  }

  private StudentResponse createStudent(UUID groupId, String email, String password) {
    // std and promotion are generated server-side (std from the entry year, promotion derived from
    // the selected group); the request deliberately omits them so a client can never supply them.
    return webTestClient
        .put()
        .uri("/students")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            StudentRequest.builder()
                .firstname("Student")
                .lastname("E" + UUID.randomUUID().toString().substring(0, 8))
                .email(email)
                .password(password)
                .groupId(groupId)
                .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(StudentResponse.class)
        .returnResult()
        .getResponseBody();
  }

  private String studentToken(String email, String password) {
    return webTestClient
        .post()
        .uri("/auth/login")
        .bodyValue(new LoginRequest(email, password))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(AuthResponse.class)
        .returnResult()
        .getResponseBody()
        .token();
  }

  @Test
  void creatingAStudentGeneratesAStdRefMatchingTheEntryYear() {
    var promotion = createPromotion(2024);
    var group = createGroup(promotion.id());

    var student = createStudent(group.id(), uniqueEmail(), "password123");

    assertNotNull(student.std());
    assertTrue(student.std().startsWith("STD24"));
    assertEquals(8, student.std().length()); // STD + 2 digit year + 3 digit sequence
    assertEquals(promotion.id(), student.promotionId());
    assertEquals(group.id(), student.currentGroupId());
  }

  @Test
  void sequenceNumberIncrementsPerEntryYear() {
    var promotion = createPromotion(2024);
    var group = createGroup(promotion.id());

    var first = createStudent(group.id(), uniqueEmail(), "password123");
    var second = createStudent(group.id(), uniqueEmail(), "password123");

    int firstSeq = Integer.parseInt(first.std().substring(5));
    int secondSeq = Integer.parseInt(second.std().substring(5));
    assertEquals(firstSeq + 1, secondSeq);
  }

  @Test
  void studentCanReadTheirOwnProfile() {
    var promotion = createPromotion(2024);
    var group = createGroup(promotion.id());
    var email = uniqueEmail();
    var student = createStudent(group.id(), email, "password123");

    var fetched =
        webTestClient
            .get()
            .uri("/students/" + student.id())
            .header("Authorization", "Bearer " + studentToken(email, "password123"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(StudentResponse.class)
            .returnResult()
            .getResponseBody();

    assertEquals(student.id(), fetched.id());
  }

  @Test
  void studentCannotReadAnotherStudentsProfile() {
    var promotion = createPromotion(2024);
    var group = createGroup(promotion.id());
    var emailA = uniqueEmail();
    var emailB = uniqueEmail();
    var studentA = createStudent(group.id(), emailA, "password123");
    createStudent(group.id(), emailB, "password123");

    webTestClient
        .get()
        .uri("/students/" + studentA.id())
        .header("Authorization", "Bearer " + studentToken(emailB, "password123"))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void studentCannotListAllStudents() {
    var promotion = createPromotion(2024);
    var group = createGroup(promotion.id());
    var email = uniqueEmail();
    createStudent(group.id(), email, "password123");

    webTestClient
        .get()
        .uri("/students")
        .header("Authorization", "Bearer " + studentToken(email, "password123"))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void movingAStudentToANewGroupUpdatesTheirCurrentGroupAndKeepsHistory() {
    var promotion = createPromotion(2024);
    var groupA = createGroup(promotion.id());
    var groupB = createGroup(promotion.id());
    var email = uniqueEmail();
    var student = createStudent(groupA.id(), email, "password123");

    webTestClient
        .put()
        .uri("/students/" + student.id() + "/group_flows")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(new MoveStudentGroupRequest(groupB.id()))
        .exchange()
        .expectStatus()
        .isOk();

    var refreshed =
        webTestClient
            .get()
            .uri("/students/" + student.id())
            .header("Authorization", "Bearer " + adminToken())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(StudentResponse.class)
            .returnResult()
            .getResponseBody();

    assertEquals(groupB.id(), refreshed.currentGroupId());
  }

  @Test
  void movingStudentToAGroupOfAnotherPromotionUpdatesTheirPromotion() {
    var promotion2024 = createPromotion(2024);
    var group2024 = createGroup(promotion2024.id());
    var promotion2025 = createPromotion(2025);
    var group2025 = createGroup(promotion2025.id());
    var email = uniqueEmail();
    var student = createStudent(group2024.id(), email, "password123");

    assertEquals(promotion2024.id(), student.promotionId());
    var stdBefore = student.std();

    webTestClient
        .put()
        .uri("/students/" + student.id() + "/group_flows")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(new MoveStudentGroupRequest(group2025.id()))
        .exchange()
        .expectStatus()
        .isOk();

    var refreshed =
        webTestClient
            .get()
            .uri("/students/" + student.id())
            .header("Authorization", "Bearer " + adminToken())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(StudentResponse.class)
            .returnResult()
            .getResponseBody();

    assertEquals(group2025.id(), refreshed.currentGroupId());
    assertEquals(promotion2025.id(), refreshed.promotionId());
    assertEquals(stdBefore, refreshed.std());
  }

  @Test
  void movingStudentWithinTheSamePromotionKeepsTheirPromotion() {
    var promotion = createPromotion(2024);
    var groupA = createGroup(promotion.id());
    var groupB = createGroup(promotion.id());
    var email = uniqueEmail();
    var student = createStudent(groupA.id(), email, "password123");

    webTestClient
        .put()
        .uri("/students/" + student.id() + "/group_flows")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(new MoveStudentGroupRequest(groupB.id()))
        .exchange()
        .expectStatus()
        .isOk();

    var refreshed =
        webTestClient
            .get()
            .uri("/students/" + student.id())
            .header("Authorization", "Bearer " + adminToken())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(StudentResponse.class)
            .returnResult()
            .getResponseBody();

    assertEquals(promotion.id(), refreshed.promotionId());
  }

  @Test
  void creatingStudentWithoutInitialGroupIsRejected() {
    webTestClient
        .put()
        .uri("/students")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            StudentRequest.builder()
                .firstname("Student")
                .lastname("Solo")
                .email(uniqueEmail())
                .password("password123")
                .build())
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void creatingStudentWithoutPasswordIsRejected() {
    var promotion = createPromotion(2024);
    var group = createGroup(promotion.id());

    webTestClient
        .put()
        .uri("/students")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            StudentRequest.builder()
                .firstname("Student")
                .lastname("NoPass")
                .email(uniqueEmail())
                .groupId(group.id())
                .build())
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void updatingStudentWithGroupIdIsRejected() {
    var promotion = createPromotion(2024);
    var group = createGroup(promotion.id());
    var student = createStudent(group.id(), uniqueEmail(), "password123");

    webTestClient
        .put()
        .uri("/students")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            StudentRequest.builder()
                .id(student.id())
                .firstname("Student")
                .lastname("Moved")
                .email(student.email())
                .groupId(group.id())
                .build())
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void unauthenticatedPutIsRejected() {
    webTestClient
        .put()
        .uri("/students")
        .bodyValue(
            StudentRequest.builder()
                .firstname("Student")
                .lastname("Anon")
                .email(uniqueEmail())
                .password("password123")
                .build())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  private String uniqueEmail() {
    return "student-" + UUID.randomUUID().toString().substring(0, 8) + "@hei.school";
  }
}
