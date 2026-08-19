package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.CurriculumStatusResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.CourseRepository;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.GroupRepository;
import org.cocojojo.mg.repository.PromotionRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JGroupFlow;
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

class CourseAssignmentIT extends FacadeIT {

  @Autowired private AdminRepository adminRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private GroupFlowRepository groupFlowRepository;
  @Autowired private CourseAssignmentRepository courseAssignmentRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @LocalServerPort int port;
  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    courseAssignmentRepository.deleteAll();
    groupFlowRepository.deleteAll();
    teacherRepository.deleteAll();
    studentRepository.deleteAll();
    adminRepository.deleteAll();
    groupRepository.deleteAll();
    promotionRepository.deleteAll();
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
    return createStudent(promotion, "student-" + UUID.randomUUID() + "@hei.school");
  }

  private JStudent createStudent(JPromotion promotion, String email) {
    return studentRepository.save(
        JStudent.builder()
            .firstname("Grace")
            .lastname("Hopper")
            .email(email)
            .password(passwordEncoder.encode("secret123"))
            .std("STD" + UUID.randomUUID())
            .promotion(promotion)
            .build());
  }

  private void joinGroup(JStudent student, JGroup group) {
    groupFlowRepository.save(
        JGroupFlow.builder()
            .student(student)
            .group(group)
            .groupFlowType(GroupFlowType.JOIN)
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
                List.of(
                    assignmentRequest(
                        course.getId(),
                        group.getId(),
                        List.of(teacher.getId()),
                        course.getCredits())))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(CourseAssignmentResponse.class)
            .returnResult()
            .getResponseBody()
            .get(0);

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
            List.of(
                assignmentRequest(
                    course.getId(), group.getId(), List.of(student.getId()), course.getCredits())))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void duplicateAssignmentIsRejected() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var token = adminToken();
    var request =
        List.of(
            assignmentRequest(
                course.getId(), group.getId(), List.of(teacher.getId()), course.getCredits()));

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
                List.of(
                    assignmentRequest(
                        course.getId(),
                        group.getId(),
                        List.of(teacherA.getId(), teacherB.getId()),
                        course.getCredits())))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(CourseAssignmentResponse.class)
            .returnResult()
            .getResponseBody()
            .get(0);

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
            List.of(
                assignmentRequest(
                    course.getId(), group.getId(), List.of(teacher.getId()), course.getCredits())))
        .exchange()
        .expectStatus()
        .isOk();

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + token2)
        .bodyValue(
            List.of(
                assignmentRequest(
                    otherCourse.getId(),
                    group.getId(),
                    List.of(otherTeacher.getId()),
                    otherCourse.getCredits())))
        .exchange()
        .expectStatus()
        .isOk();

    var results =
        webTestClient
            .get()
            .uri("/course-assignments?teacherId=" + otherTeacher.getId())
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new ParameterizedTypeReference<TestPage<CourseAssignmentResponse>>() {})
            .returnResult()
            .getResponseBody()
            .content();

    assertNotNull(results);
    assertTrue(
        results.stream()
            .allMatch(a -> a.teachers().stream().anyMatch(t -> t.id().equals(teacher.getId()))));
    assertTrue(
        results.stream()
            .noneMatch(
                a -> a.teachers().stream().anyMatch(t -> t.id().equals(otherTeacher.getId()))));
  }

  @Test
  void teacherCannotAccessAnotherTeachersAssignmentById() {
    var teacher = createTeacher("teacher-owner-" + UUID.randomUUID() + "@hei.school");
    var otherTeacher = createTeacher();
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var token = adminToken();

    var assignment =
        webTestClient
            .put()
            .uri("/course-assignments")
            .header("Authorization", "Bearer " + token)
            .bodyValue(
                List.of(
                    assignmentRequest(
                        course.getId(),
                        group.getId(),
                        List.of(otherTeacher.getId()),
                        course.getCredits())))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(CourseAssignmentResponse.class)
            .returnResult()
            .getResponseBody()
            .get(0);

    webTestClient
        .get()
        .uri("/course-assignments/" + assignment.id())
        .header("Authorization", "Bearer " + loginToken(teacher.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void studentCanOnlySeeAssignmentsOfTheirCurrentGroup() {
    var promotion = createPromotion();
    var groupA = createGroup(promotion);
    var groupB = createGroup(promotion);
    var courseA = createCourse();
    var courseB = createCourse();
    var teacher = createTeacher();
    var student = createStudent(promotion, "student-owner@hei.school");
    joinGroup(student, groupA);
    var token = adminToken();
    var studentToken = loginToken(student.getEmail(), "secret123");

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + token)
        .bodyValue(
            List.of(
                assignmentRequest(
                    courseA.getId(),
                    groupA.getId(),
                    List.of(teacher.getId()),
                    courseA.getCredits())))
        .exchange()
        .expectStatus()
        .isOk();

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + token)
        .bodyValue(
            List.of(
                assignmentRequest(
                    courseB.getId(),
                    groupB.getId(),
                    List.of(teacher.getId()),
                    courseB.getCredits())))
        .exchange()
        .expectStatus()
        .isOk();

    var results =
        webTestClient
            .get()
            .uri("/course-assignments")
            .header("Authorization", "Bearer " + studentToken)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new ParameterizedTypeReference<TestPage<CourseAssignmentResponse>>() {})
            .returnResult()
            .getResponseBody()
            .content();

    assertNotNull(results);
    assertTrue(results.stream().allMatch(a -> a.groupId().equals(groupA.getId())));
  }

  @Test
  void adminCanFilterAssignmentsByCourse() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var otherCourse = createCourse();
    var teacher = createTeacher();
    var token = adminToken();

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + token)
        .bodyValue(
            List.of(
                assignmentRequest(
                    course.getId(), group.getId(), List.of(teacher.getId()), course.getCredits())))
        .exchange()
        .expectStatus()
        .isOk();

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + token)
        .bodyValue(
            List.of(
                assignmentRequest(
                    otherCourse.getId(),
                    group.getId(),
                    List.of(teacher.getId()),
                    otherCourse.getCredits())))
        .exchange()
        .expectStatus()
        .isOk();

    var results =
        webTestClient
            .get()
            .uri("/course-assignments?courseId=" + course.getId())
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new ParameterizedTypeReference<TestPage<CourseAssignmentResponse>>() {})
            .returnResult()
            .getResponseBody()
            .content();

    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals(course.getId(), results.get(0).courseId());
  }

  @Test
  void curriculumStatusIsAdminOnly() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var student = createStudent(promotion);
    var token = adminToken();

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + token)
        .bodyValue(
            List.of(
                assignmentRequest(
                    course.getId(), group.getId(), List.of(teacher.getId()), course.getCredits())))
        .exchange()
        .expectStatus()
        .isOk();

    var uri =
        "/course-assignments/curriculum-status?groupId="
            + group.getId()
            + "&academicYear=2024&semester=S1";

    var status =
        webTestClient
            .get()
            .uri(uri)
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(CurriculumStatusResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(status);
    assertEquals(Semester.S1, status.semester());
    assertEquals(course.getCredits(), status.assignedCredits());
    assertEquals(30, status.targetCredits());

    webTestClient
        .get()
        .uri(uri)
        .header("Authorization", "Bearer " + loginToken(teacher.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isForbidden();

    webTestClient
        .get()
        .uri(uri)
        .header("Authorization", "Bearer " + loginToken(student.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void creditTargetRejectsOverloadedSemesterWithoutPersisting() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var teacher = createTeacher();
    var token = adminToken();
    var heavyCourse =
        courseRepository.save(
            JCourse.builder()
                .code("UE-" + UUID.randomUUID())
                .name("Heavy " + UUID.randomUUID())
                .credits(20)
                .totalHours(40)
                .studentLevel(StudentLevel.L1)
                .build());
    var otherCourse =
        courseRepository.save(
            JCourse.builder()
                .code("UE-" + UUID.randomUUID())
                .name("Other " + UUID.randomUUID())
                .credits(15)
                .totalHours(30)
                .studentLevel(StudentLevel.L1)
                .build());

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + token)
        .bodyValue(
            List.of(
                assignmentRequest(heavyCourse.getId(), group.getId(), List.of(teacher.getId()), 20),
                assignmentRequest(
                    otherCourse.getId(), group.getId(), List.of(teacher.getId()), 15)))
        .exchange()
        .expectStatus()
        .isBadRequest();

    assertEquals(0, courseAssignmentRepository.count());
  }

  @Test
  void curriculumStatusCompleteIsExactTargetWithNothingMissing() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var teacher = createTeacher();
    var token = adminToken();
    var twenty =
        courseRepository.save(
            JCourse.builder()
                .code("UE-" + UUID.randomUUID())
                .name("Twenty " + UUID.randomUUID())
                .credits(20)
                .totalHours(40)
                .studentLevel(StudentLevel.L1)
                .build());
    var ten =
        courseRepository.save(
            JCourse.builder()
                .code("UE-" + UUID.randomUUID())
                .name("Ten " + UUID.randomUUID())
                .credits(10)
                .totalHours(20)
                .studentLevel(StudentLevel.L1)
                .build());
    var uri =
        "/course-assignments/curriculum-status?groupId="
            + group.getId()
            + "&academicYear=2024&semester=S1";

    // Below the 30-credit target: not complete.
    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + token)
        .bodyValue(
            List.of(assignmentRequest(twenty.getId(), group.getId(), List.of(teacher.getId()), 20)))
        .exchange()
        .expectStatus()
        .isOk();

    var partial =
        webTestClient
            .get()
            .uri(uri)
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(CurriculumStatusResponse.class)
            .returnResult()
            .getResponseBody();
    assertEquals(20, partial.assignedCredits());
    assertEquals(30, partial.targetCredits());
    assertFalse(partial.complete());
    assertFalse(partial.missingCourses().isEmpty());

    // Reaching exactly the 30-credit target is still not "complete" while catalog courses of the
    // level remain unassigned: complete requires the exact target AND nothing missing.
    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + token)
        .bodyValue(
            List.of(assignmentRequest(ten.getId(), group.getId(), List.of(teacher.getId()), 10)))
        .exchange()
        .expectStatus()
        .isOk();

    var atTarget =
        webTestClient
            .get()
            .uri(uri)
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(CurriculumStatusResponse.class)
            .returnResult()
            .getResponseBody();
    assertEquals(30, atTarget.assignedCredits());
    assertEquals(30, atTarget.targetCredits());
    assertFalse(atTarget.complete());
    assertFalse(atTarget.missingCourses().isEmpty());
  }

  @Test
  void onlyAdminCanUpsertAssignments() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var student = createStudent(promotion);
    var payload =
        List.of(
            assignmentRequest(
                course.getId(), group.getId(), List.of(teacher.getId()), course.getCredits()));

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + loginToken(teacher.getEmail(), "secret123"))
        .bodyValue(payload)
        .exchange()
        .expectStatus()
        .isForbidden();

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + loginToken(student.getEmail(), "secret123"))
        .bodyValue(payload)
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void onlyAdminCanDeleteAssignments() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var student = createStudent(promotion);
    var token = adminToken();

    var assignment =
        webTestClient
            .put()
            .uri("/course-assignments")
            .header("Authorization", "Bearer " + token)
            .bodyValue(
                List.of(
                    assignmentRequest(
                        course.getId(),
                        group.getId(),
                        List.of(teacher.getId()),
                        course.getCredits())))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(CourseAssignmentResponse.class)
            .returnResult()
            .getResponseBody()
            .get(0);

    webTestClient
        .delete()
        .uri("/course-assignments/" + assignment.id())
        .header("Authorization", "Bearer " + loginToken(teacher.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isForbidden();

    webTestClient
        .delete()
        .uri("/course-assignments/" + assignment.id())
        .header("Authorization", "Bearer " + loginToken(student.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isForbidden();

    webTestClient
        .delete()
        .uri("/course-assignments/" + assignment.id())
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus()
        .isNoContent();

    webTestClient
        .get()
        .uri("/course-assignments/" + assignment.id())
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void studentWithNoGroupGetsEmptyList() {
    var promotion = createPromotion();
    var student = createStudent(promotion, "student-nogroup@hei.school");
    var studentToken = loginToken(student.getEmail(), "secret123");

    var results =
        webTestClient
            .get()
            .uri("/course-assignments")
            .header("Authorization", "Bearer " + studentToken)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new ParameterizedTypeReference<TestPage<CourseAssignmentResponse>>() {})
            .returnResult()
            .getResponseBody()
            .content();

    assertNotNull(results);
    assertTrue(results.isEmpty());
  }

  @Test
  void unauthenticatedCannotAccessCourseAssignments() {
    webTestClient.get().uri("/course-assignments").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void teacherCanAccessOwnAssignmentById() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var token = adminToken();

    var assignment =
        webTestClient
            .put()
            .uri("/course-assignments")
            .header("Authorization", "Bearer " + token)
            .bodyValue(
                List.of(
                    assignmentRequest(
                        course.getId(),
                        group.getId(),
                        List.of(teacher.getId()),
                        course.getCredits())))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(CourseAssignmentResponse.class)
            .returnResult()
            .getResponseBody()
            .get(0);

    var fetched =
        webTestClient
            .get()
            .uri("/course-assignments/" + assignment.id())
            .header("Authorization", "Bearer " + loginToken(teacher.getEmail(), "secret123"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(CourseAssignmentResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(fetched);
    assertEquals(assignment.id(), fetched.id());
  }

  @Test
  void studentCanAccessOwnGroupAssignmentById() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var student = createStudent(promotion, "student-owner-" + UUID.randomUUID() + "@hei.school");
    joinGroup(student, group);
    var token = adminToken();

    var assignment =
        webTestClient
            .put()
            .uri("/course-assignments")
            .header("Authorization", "Bearer " + token)
            .bodyValue(
                List.of(
                    assignmentRequest(
                        course.getId(),
                        group.getId(),
                        List.of(teacher.getId()),
                        course.getCredits())))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(CourseAssignmentResponse.class)
            .returnResult()
            .getResponseBody()
            .get(0);

    var fetched =
        webTestClient
            .get()
            .uri("/course-assignments/" + assignment.id())
            .header("Authorization", "Bearer " + loginToken(student.getEmail(), "secret123"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(CourseAssignmentResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(fetched);
    assertEquals(group.getId(), fetched.groupId());
  }

  @Test
  void studentCannotAccessAnotherGroupsAssignmentById() {
    var promotion = createPromotion();
    var groupA = createGroup(promotion);
    var groupB = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var student = createStudent(promotion, "student-owner-" + UUID.randomUUID() + "@hei.school");
    joinGroup(student, groupA);
    var token = adminToken();

    var assignment =
        webTestClient
            .put()
            .uri("/course-assignments")
            .header("Authorization", "Bearer " + token)
            .bodyValue(
                List.of(
                    assignmentRequest(
                        course.getId(),
                        groupB.getId(),
                        List.of(teacher.getId()),
                        course.getCredits())))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(CourseAssignmentResponse.class)
            .returnResult()
            .getResponseBody()
            .get(0);

    webTestClient
        .get()
        .uri("/course-assignments/" + assignment.id())
        .header("Authorization", "Bearer " + loginToken(student.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void adminCanUpdateExistingAssignment() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var token = adminToken();

    var created =
        webTestClient
            .put()
            .uri("/course-assignments")
            .header("Authorization", "Bearer " + token)
            .bodyValue(
                List.of(
                    assignmentRequest(
                        course.getId(),
                        group.getId(),
                        List.of(teacher.getId()),
                        course.getCredits())))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(CourseAssignmentResponse.class)
            .returnResult()
            .getResponseBody()
            .get(0);

    var updated =
        webTestClient
            .put()
            .uri("/course-assignments")
            .header("Authorization", "Bearer " + token)
            .bodyValue(
                List.of(
                    CourseAssignmentRequest.builder()
                        .id(created.id())
                        .courseId(course.getId())
                        .groupId(group.getId())
                        .teacherIds(List.of(teacher.getId()))
                        .academicYear(2024)
                        .semester(Semester.S1)
                        .credits(8)
                        .build()))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(CourseAssignmentResponse.class)
            .returnResult()
            .getResponseBody()
            .get(0);

    assertEquals(created.id(), updated.id());
    assertEquals(8, updated.credits());
  }

  @Test
  void courseLevelMustMatchSemester() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var teacher = createTeacher();
    var l2Course =
        courseRepository.save(
            JCourse.builder()
                .code("UE-" + UUID.randomUUID())
                .name("Course " + UUID.randomUUID())
                .credits(5)
                .totalHours(20)
                .studentLevel(StudentLevel.L2)
                .build());

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            List.of(
                assignmentRequest(
                    l2Course.getId(),
                    group.getId(),
                    List.of(teacher.getId()),
                    l2Course.getCredits())))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void trackIncompatibleAssignmentIsRejected() {
    var promotion = createPromotion();
    var teacher = createTeacher();
    var group =
        groupRepository.save(
            JGroup.builder()
                .ref("GRP-" + UUID.randomUUID())
                .promotion(promotion)
                .track(Track.TN)
                .build());
    var course =
        courseRepository.save(
            JCourse.builder()
                .code("UE-" + UUID.randomUUID())
                .name("Course " + UUID.randomUUID())
                .credits(5)
                .totalHours(20)
                .studentLevel(StudentLevel.L1)
                .track(Track.EL)
                .build());

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            List.of(
                assignmentRequest(
                    course.getId(), group.getId(), List.of(teacher.getId()), course.getCredits())))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void trackSpecificCourseOnNullTrackGroupIsRejected() {
    // A track-specific course must not be assignable to an L1-shaped group whose track is null;
    // the old check silently no-oped because of a group.track() != null guard.
    var promotion = createPromotion();
    var teacher = createTeacher();
    var group =
        groupRepository.save(
            JGroup.builder().ref("GRP-" + UUID.randomUUID()).promotion(promotion).build());
    var course =
        courseRepository.save(
            JCourse.builder()
                .code("UE-" + UUID.randomUUID())
                .name("Course " + UUID.randomUUID())
                .credits(5)
                .totalHours(20)
                .studentLevel(StudentLevel.L1)
                .track(Track.EL)
                .build());

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            List.of(
                assignmentRequest(
                    course.getId(), group.getId(), List.of(teacher.getId()), course.getCredits())))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void commonCourseOnTrackGroupIsStillAllowed() {
    // Regression: courses without a track remain assignable to any group, including track ones.
    var promotion = createPromotion();
    var teacher = createTeacher();
    var group =
        groupRepository.save(
            JGroup.builder()
                .ref("GRP-" + UUID.randomUUID())
                .promotion(promotion)
                .track(Track.TN)
                .build());
    var course =
        courseRepository.save(
            JCourse.builder()
                .code("UE-" + UUID.randomUUID())
                .name("Course " + UUID.randomUUID())
                .credits(5)
                .totalHours(20)
                .studentLevel(StudentLevel.L1)
                .build());

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            List.of(
                assignmentRequest(
                    course.getId(), group.getId(), List.of(teacher.getId()), course.getCredits())))
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void unknownTeacherIdIsRejected() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            List.of(
                assignmentRequest(
                    course.getId(),
                    group.getId(),
                    List.of(UUID.randomUUID()),
                    course.getCredits())))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void unknownCourseIdIsRejected() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var teacher = createTeacher();

    webTestClient
        .put()
        .uri("/course-assignments")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(
            List.of(
                assignmentRequest(UUID.randomUUID(), group.getId(), List.of(teacher.getId()), 5)))
        .exchange()
        .expectStatus()
        .isNotFound();
  }
}
