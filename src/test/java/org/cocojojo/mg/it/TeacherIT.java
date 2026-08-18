package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.TeacherRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.TeacherResponse;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.PromotionRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.repository.model.JTeacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class TeacherIT extends FacadeIT {

  @Autowired private AdminRepository adminRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private JwtService jwtService;
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

  private String studentToken() {
    var promotion =
        promotionRepository.save(
            JPromotion.builder()
                .ref("TI-PROMO-" + UUID.randomUUID().toString().substring(0, 8))
                .name("TI Promotion")
                .entryYear(2023)
                .build());
    var student =
        studentRepository.save(
            JStudent.builder()
                .firstname("Alan")
                .lastname("Turing")
                .email(
                    "teacher-student-"
                        + UUID.randomUUID().toString().substring(0, 8)
                        + "@hei.school")
                .password(passwordEncoder.encode("secret123"))
                .std("TI-STD-" + UUID.randomUUID().toString().substring(0, 8))
                .promotion(promotion)
                .build());
    return jwtService.generateToken(student.getId(), student.getEmail(), Role.STUDENT);
  }

  private String teacherToken() {
    var teacher =
        teacherRepository.save(
            JTeacher.builder()
                .firstname("Grace")
                .lastname("Hopper")
                .email(uniqueEmail())
                .password(passwordEncoder.encode("secret123"))
                .build());
    return jwtService.generateToken(teacher.getId(), teacher.getEmail(), Role.TEACHER);
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
    var emailA = uniqueEmail();
    var emailB = uniqueEmail();
    createTeacher(emailA);
    createTeacher(emailB);

    var teachers =
        webTestClient
            .get()
            .uri("/teachers")
            .header("Authorization", "Bearer " + adminToken())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new ParameterizedTypeReference<TestPage<TeacherResponse>>() {})
            .returnResult()
            .getResponseBody()
            .content()
            .stream()
            .filter(t -> t.email().equals(emailA) || t.email().equals(emailB))
            .toList();

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
  void studentCannotReadATeacherProfile() {
    var teacher = createTeacher(uniqueEmail());

    webTestClient
        .get()
        .uri("/teachers/" + teacher.id())
        .header("Authorization", "Bearer " + studentToken())
        .exchange()
        .expectStatus()
        .isForbidden();
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

  @Test
  void adminCanDeleteATeacher() {
    var teacher = createTeacher(uniqueEmail());

    webTestClient
        .delete()
        .uri("/teachers/" + teacher.id())
        .header("Authorization", "Bearer " + adminToken())
        .exchange()
        .expectStatus()
        .isNoContent();

    webTestClient
        .get()
        .uri("/teachers/" + teacher.id())
        .header("Authorization", "Bearer " + adminToken())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void deletingAnUnknownTeacherReturnsNotFound() {
    webTestClient
        .delete()
        .uri("/teachers/" + UUID.randomUUID())
        .header("Authorization", "Bearer " + adminToken())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void teacherCannotDeleteATeacher() {
    var teacher = createTeacher(uniqueEmail());

    webTestClient
        .delete()
        .uri("/teachers/" + teacher.id())
        .header("Authorization", "Bearer " + teacherToken())
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  private String uniqueEmail() {
    return "teacher-" + UUID.randomUUID().toString().substring(0, 8) + "@hei.school";
  }
}
