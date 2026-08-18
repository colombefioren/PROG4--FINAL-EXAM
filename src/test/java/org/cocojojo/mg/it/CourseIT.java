package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.CourseRepository;
import org.cocojojo.mg.repository.GroupRepository;
import org.cocojojo.mg.repository.PromotionRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class CourseIT extends FacadeIT {

  @Autowired private AdminRepository adminRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private JwtService jwtService;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private CourseRepository courseRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private CourseAssignmentRepository courseAssignmentRepository;

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

  private String studentToken() {
    var promotion =
        promotionRepository.save(
            JPromotion.builder()
                .ref("CI-PROMO-" + UUID.randomUUID().toString().substring(0, 8))
                .name("CI Promotion " + UUID.randomUUID().toString().substring(0, 8))
                .entryYear(2023)
                .build());
    var student =
        studentRepository.save(
            JStudent.builder()
                .firstname("Alan")
                .lastname("Turing")
                .email(
                    "course-student-"
                        + UUID.randomUUID().toString().substring(0, 8)
                        + "@hei.school")
                .password(passwordEncoder.encode("secret123"))
                .std("CI-STD-" + UUID.randomUUID().toString().substring(0, 8))
                .promotion(promotion)
                .build());
    return jwtService.generateToken(student.getId(), student.getEmail(), Role.STUDENT);
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
            .uri("/courses?size=1000")
            .header("Authorization", "Bearer " + adminToken())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new ParameterizedTypeReference<TestPage<CourseResponse>>() {})
            .returnResult()
            .getResponseBody()
            .content();

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
  void studentCannotBrowseCourseCatalog() {
    webTestClient
        .get()
        .uri("/courses")
        .header("Authorization", "Bearer " + studentToken())
        .exchange()
        .expectStatus()
        .isForbidden();
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

  @Test
  void adminCanDeleteACourse() {
    var code = uniqueCode();
    var created =
        webTestClient
            .put()
            .uri("/courses")
            .header("Authorization", "Bearer " + adminToken())
            .bodyValue(
                CourseRequest.builder()
                    .code(code)
                    .name("Doomed course")
                    .credits(4)
                    .studentLevel(StudentLevel.L1)
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(CourseResponse.class)
            .returnResult()
            .getResponseBody();

    webTestClient
        .delete()
        .uri("/courses/" + created.id())
        .header("Authorization", "Bearer " + adminToken())
        .exchange()
        .expectStatus()
        .isNoContent();

    webTestClient
        .get()
        .uri("/courses/" + created.id())
        .header("Authorization", "Bearer " + adminToken())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void deletingAnUnknownCourseReturnsNotFound() {
    webTestClient
        .delete()
        .uri("/courses/" + UUID.randomUUID())
        .header("Authorization", "Bearer " + adminToken())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void studentCannotDeleteACourse() {
    var course = saveCourse();

    webTestClient
        .delete()
        .uri("/courses/" + course.getId())
        .header("Authorization", "Bearer " + studentToken())
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void deletingAnAssignedCourseReturnsConflict() {
    var course = saveCourse();
    saveAssignment(course);

    webTestClient
        .delete()
        .uri("/courses/" + course.getId())
        .header("Authorization", "Bearer " + adminToken())
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.CONFLICT);

    webTestClient
        .get()
        .uri("/courses/" + course.getId())
        .header("Authorization", "Bearer " + adminToken())
        .exchange()
        .expectStatus()
        .isOk();
  }

  private JCourse saveCourse() {
    return courseRepository.save(
        JCourse.builder()
            .code(uniqueCode())
            .name("Course " + UUID.randomUUID().toString().substring(0, 8))
            .credits(4)
            .studentLevel(StudentLevel.L1)
            .build());
  }

  private JGroup saveGroup() {
    var promotion =
        promotionRepository.save(
            JPromotion.builder()
                .ref("CI-PROMO-" + UUID.randomUUID().toString().substring(0, 8))
                .name("CI Group Promotion")
                .entryYear(2025)
                .build());
    return groupRepository.save(
        JGroup.builder()
            .promotion(promotion)
            .ref("CI-G-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .track(Track.TN)
            .build());
  }

  private JCourseAssignment saveAssignment(JCourse course) {
    return courseAssignmentRepository.save(
        JCourseAssignment.builder()
            .course(course)
            .group(saveGroup())
            .teachers(List.of())
            .academicYear(2025)
            .semester(Semester.S1)
            .credits(4)
            .build());
  }
}
