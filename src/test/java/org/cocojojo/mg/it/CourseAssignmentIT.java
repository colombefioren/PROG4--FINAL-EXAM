package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.CourseRepository;
import org.cocojojo.mg.repository.GroupRepository;
import org.cocojojo.mg.repository.PromotionRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.repository.model.JTeacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class CourseAssignmentIT extends FacadeIT {

  @Autowired private AdminRepository adminRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private CourseAssignmentRepository courseAssignmentRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @LocalServerPort int port;
  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    courseAssignmentRepository.deleteAll();
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

  private String adminToken() {
    var email = "admin-" + UUID.randomUUID() + "@hei.school";
    adminRepository.save(
        JAdmin.builder()
            .firstname("Ada")
            .lastname("Lovelace")
            .email(email)
            .password(passwordEncoder.encode("secret123"))
            .build());
    return loginToken(email, "secret123");
  }

  private JPromotion createPromotion() {
    return promotionRepository.save(
        JPromotion.builder()
            .ref("PROMO-" + UUID.randomUUID())
            .name("PROMO-" + UUID.randomUUID())
            .entryYear(2024)
            .build());
  }

  private JGroup createGroup(JPromotion promotion) {
    return groupRepository.save(
        JGroup.builder().ref("GRP-" + UUID.randomUUID()).promotion(promotion).build());
  }

  private JCourse createCourse() {
    return courseRepository.save(
        JCourse.builder()
            .code("UE-" + UUID.randomUUID())
            .name("Course " + UUID.randomUUID())
            .credits(5)
            .totalHours(20)
            .studentLevel(StudentLevel.L1)
            .build());
  }

  private JTeacher createTeacher() {
    return createTeacher("teacher-" + UUID.randomUUID() + "@hei.school");
  }

  private JTeacher createTeacher(String email) {
    return teacherRepository.save(
        JTeacher.builder()
            .firstname("Alan")
            .lastname("Turing")
            .email(email)
            .password(passwordEncoder.encode("secret123"))
            .build());
  }

  private JStudent createStudent(JPromotion promotion) {
    return studentRepository.save(
        JStudent.builder()
            .firstname("Grace")
            .lastname("Hopper")
            .email("student-" + UUID.randomUUID() + "@hei.school")
            .password(passwordEncoder.encode("secret123"))
            .std("STD" + UUID.randomUUID())
            .promotion(promotion)
            .build());
  }

  private CourseAssignmentRequest assignmentRequest(
      UUID courseId, UUID groupId, List<UUID> teacherIds, int credits) {
    return CourseAssignmentRequest.builder()
        .courseId(courseId)
        .groupId(groupId)
        .teacherIds(teacherIds)
        .academicYear(2024)
        .semester(Semester.S1)
        .credits(credits)
        .build();
  }

  @Test
  void adminCanCreateACourseAssignment() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();

    var assignment =
        webTestClient
            .put()
            .uri("/course-assignments")
            .header("Authorization", "Bearer " + adminToken())
            .bodyValue(
                assignmentRequest(
                    course.getId(), group.getId(), List.of(teacher.getId()), course.getCredits()))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(CourseAssignmentResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(assignment);
    assertNotNull(assignment.id());
    assertEquals(course.getId(), assignment.courseId());
    assertEquals(group.getId(), assignment.groupId());
    assertEquals(course.getCredits(), assignment.credits());
    assertEquals(1, assignment.teachers().size());
  }

  @Test
  void cannotAssignAStudentAsTeacher() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var student = createStudent(promotion);

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            assignmentRequest(
                course.getId(), group.getId(), List.of(student.getId()), course.getCredits()))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void duplicateAssignmentIsRejected() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var token = adminToken();
    var request =
        assignmentRequest(
            course.getId(), group.getId(), List.of(teacher.getId()), course.getCredits());

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + token)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk();

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + token)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void sameCourseCanBeCoTaughtByTwoTeachers() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacherA = createTeacher();
    var teacherB = createTeacher();

    var assignment =
        webTestClient
            .put()
            .uri("/course-assignments")
            .header("Authorization", "Bearer " + adminToken())
            .bodyValue(
                assignmentRequest(
                    course.getId(),
                    group.getId(),
                    List.of(teacherA.getId(), teacherB.getId()),
                    course.getCredits()))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(CourseAssignmentResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(assignment);
    assertEquals(2, assignment.teachers().size());
    assertTrue(assignment.teachers().stream().anyMatch(t -> t.id().equals(teacherA.getId())));
    assertTrue(assignment.teachers().stream().anyMatch(t -> t.id().equals(teacherB.getId())));
  }

  @Test
  void teacherScopedCourseAssignmentListIgnoresQueryParamOverride() {
    var teacher = createTeacher("teacher-owner@hei.school");
    var otherTeacher = createTeacher();
    var token = loginToken(teacher.getEmail(), "secret123");

    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var otherCourse = createCourse();
    var token2 = adminToken();

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + token2)
        .bodyValue(
            assignmentRequest(
                course.getId(), group.getId(), List.of(teacher.getId()), course.getCredits()))
        .exchange()
        .expectStatus()
        .isOk();

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + token2)
        .bodyValue(
            assignmentRequest(
                otherCourse.getId(),
                group.getId(),
                List.of(otherTeacher.getId()),
                otherCourse.getCredits()))
        .exchange()
        .expectStatus()
        .isOk();

    var results =
        webTestClient
            .get()
            .uri("/course-assignments?teacher_id=" + otherTeacher.getId())
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(CourseAssignmentResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(results);
    assertTrue(
        results.stream()
            .allMatch(a -> a.teachers().stream().anyMatch(t -> t.id().equals(teacher.getId()))));
    assertTrue(
        results.stream()
            .noneMatch(
                a -> a.teachers().stream().anyMatch(t -> t.id().equals(otherTeacher.getId()))));
  }
}
