package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.TeacherRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.TeacherResponse;
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
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JGroup;
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
  @Autowired private CourseRepository courseRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private CourseAssignmentRepository courseAssignmentRepository;
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

  @Test
  void adminCanDeleteATeacherThatIsAssignedToACourse() {
    var teacher = createTeacher(uniqueEmail());
    saveAssignment(teacherRepository.getReferenceById(teacher.id()));

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

  private JCourse saveCourse() {
    return courseRepository.save(
        JCourse.builder()
            .code("TI-C-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .name("TI Course")
            .credits(4)
            .studentLevel(StudentLevel.L3)
            .build());
  }

  private JGroup saveGroup() {
    return groupRepository.save(
        JGroup.builder()
            .promotion(saveAssignmentPromotion())
            .ref("TI-G-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .track(Track.TN)
            .build());
  }

  private JPromotion saveAssignmentPromotion() {
    return promotionRepository.save(
        JPromotion.builder()
            .ref("TI-PROMO-" + UUID.randomUUID().toString().substring(0, 8))
            .name("TI Assignment Promotion " + UUID.randomUUID().toString().substring(0, 8))
            .entryYear(2025)
            .build());
  }

  private JCourseAssignment saveAssignment(JTeacher teacher) {
    return courseAssignmentRepository.save(
        JCourseAssignment.builder()
            .course(saveCourse())
            .group(saveGroup())
            .teachers(List.of(teacher))
            .academicYear(2025)
            .semester(Semester.S1)
            .credits(4)
            .build());
  }

  private String uniqueEmail() {
    return "teacher-" + UUID.randomUUID().toString().substring(0, 8) + "@hei.school";
  }
}
