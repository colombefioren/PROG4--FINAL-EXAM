package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.ExamRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.ExamResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.model.Fraction;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.CourseRepository;
import org.cocojojo.mg.repository.ExamRepository;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.GroupRepository;
import org.cocojojo.mg.repository.PromotionRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JGroupFlow;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.repository.model.JTeacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class ExamIT extends FacadeIT {

  @Autowired private AdminRepository adminRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private GroupFlowRepository groupFlowRepository;
  @Autowired private CourseAssignmentRepository courseAssignmentRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @LocalServerPort int port;
  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    examRepository.deleteAll();
    courseAssignmentRepository.deleteAll();
    groupFlowRepository.deleteAll();
    teacherRepository.deleteAll();
    studentRepository.deleteAll();
    adminRepository.deleteAll();
    courseRepository.deleteAll();
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

  private JCourseAssignment createAssignment(JCourse course, JGroup group, JTeacher teacher) {
    return courseAssignmentRepository.save(
        JCourseAssignment.builder()
            .course(course)
            .group(group)
            .teachers(List.of(teacher))
            .academicYear(2024)
            .semester(Semester.S1)
            .credits(course.getCredits())
            .build());
  }

  private ExamRequest examRequest(UUID courseAssignmentId, Fraction coefficient) {
    return ExamRequest.builder()
        .courseAssignmentId(courseAssignmentId)
        .title("Exam " + UUID.randomUUID())
        .examDatetime(Instant.parse("2024-06-01T09:00:00Z"))
        .coefficient(coefficient)
        .build();
  }

  private ExamResponse createExam(String token, UUID courseAssignmentId) {
    return webTestClient
        .put()
        .uri("/course-assignments/" + courseAssignmentId + "/exams")
        .header("Authorization", "Bearer " + token)
        .bodyValue(examRequest(courseAssignmentId, new Fraction(1, 2)))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(ExamResponse.class)
        .returnResult()
        .getResponseBody();
  }

  @Test
  void adminCanCreateExam() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);

    var exam = createExam(adminToken(), assignment.getId());

    assertNotNull(exam);
    assertNotNull(exam.id());
    assertEquals(assignment.getId(), exam.courseAssignmentId());
    assertEquals(new Fraction(1, 2), exam.coefficient());
  }

  @Test
  void adminCanUpdateExam() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);
    var token = adminToken();
    var created = createExam(token, assignment.getId());

    var updated =
        webTestClient
            .put()
            .uri("/course-assignments/" + assignment.getId() + "/exams")
            .header("Authorization", "Bearer " + token)
            .bodyValue(
                ExamRequest.builder()
                    .id(created.id())
                    .courseAssignmentId(assignment.getId())
                    .title("Updated " + UUID.randomUUID())
                    .examDatetime(Instant.parse("2024-06-15T09:00:00Z"))
                    .coefficient(new Fraction(3, 4))
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(ExamResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(updated);
    assertEquals(created.id(), updated.id());
    assertEquals(new Fraction(3, 4), updated.coefficient());
  }

  @Test
  void adminCanDeleteExam() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);
    var token = adminToken();
    var created = createExam(token, assignment.getId());

    webTestClient
        .delete()
        .uri("/course-assignments/" + assignment.getId() + "/exams/" + created.id())
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus()
        .isNoContent();

    webTestClient
        .get()
        .uri("/course-assignments/" + assignment.getId() + "/exams/" + created.id())
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void adminCanListExamsForCourseAssignment() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);
    var token = adminToken();
    createExam(token, assignment.getId());
    createExam(token, assignment.getId());

    var exams =
        webTestClient
            .get()
            .uri("/course-assignments/" + assignment.getId() + "/exams")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ExamResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(exams);
    assertEquals(2, exams.size());
  }

  @Test
  void adminCanGetExamById() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);
    var token = adminToken();
    var created = createExam(token, assignment.getId());

    var fetched =
        webTestClient
            .get()
            .uri("/course-assignments/" + assignment.getId() + "/exams/" + created.id())
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(ExamResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(fetched);
    assertEquals(created.id(), fetched.id());
  }

  @Test
  void adminCanFilterExamsByDateRange() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);
    var token = adminToken();
    createExam(token, assignment.getId());

    var before =
        webTestClient
            .get()
            .uri("/course-assignments/" + assignment.getId() + "/exams?from=2024-07-01T00:00:00Z")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ExamResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(before);
    assertEquals(0, before.size());

    var within =
        webTestClient
            .get()
            .uri(
                "/course-assignments/"
                    + assignment.getId()
                    + "/exams?from=2024-01-01T00:00:00Z&to=2024-12-31T00:00:00Z")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ExamResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(within);
    assertEquals(1, within.size());
  }

  @Test
  void studentCannotUpsertExams() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);
    var student = createStudent(promotion, "student-" + UUID.randomUUID() + "@hei.school");

    webTestClient
        .put()
        .uri("/course-assignments/" + assignment.getId() + "/exams")
        .header("Authorization", "Bearer " + loginToken(student.getEmail(), "secret123"))
        .bodyValue(examRequest(assignment.getId(), new Fraction(1, 2)))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void studentCannotDeleteExams() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);
    var token = adminToken();
    var created = createExam(token, assignment.getId());
    var student = createStudent(promotion, "student-" + UUID.randomUUID() + "@hei.school");

    webTestClient
        .delete()
        .uri("/course-assignments/" + assignment.getId() + "/exams/" + created.id())
        .header("Authorization", "Bearer " + loginToken(student.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void unauthenticatedCannotAccessExams() {
    webTestClient
        .get()
        .uri("/course-assignments/" + UUID.randomUUID() + "/exams")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void teacherCanCreateExamForCourseTheyTeach() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);

    var exam =
        webTestClient
            .put()
            .uri("/course-assignments/" + assignment.getId() + "/exams")
            .header("Authorization", "Bearer " + loginToken(teacher.getEmail(), "secret123"))
            .bodyValue(examRequest(assignment.getId(), new Fraction(1, 2)))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(ExamResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(exam);
    assertEquals(assignment.getId(), exam.courseAssignmentId());
  }

  @Test
  void teacherCannotCreateExamForCourseTheyDoNotTeach() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var ownerTeacher = createTeacher();
    var otherTeacher = createTeacher();
    var assignment = createAssignment(course, group, ownerTeacher);

    webTestClient
        .put()
        .uri("/course-assignments/" + assignment.getId() + "/exams")
        .header("Authorization", "Bearer " + loginToken(otherTeacher.getEmail(), "secret123"))
        .bodyValue(examRequest(assignment.getId(), new Fraction(1, 2)))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void teacherCanUpdateExamTheyOwn() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);
    var created = createExam(adminToken(), assignment.getId());

    var updated =
        webTestClient
            .put()
            .uri("/course-assignments/" + assignment.getId() + "/exams")
            .header("Authorization", "Bearer " + loginToken(teacher.getEmail(), "secret123"))
            .bodyValue(
                ExamRequest.builder()
                    .id(created.id())
                    .courseAssignmentId(assignment.getId())
                    .title("Updated " + UUID.randomUUID())
                    .examDatetime(Instant.parse("2024-06-15T09:00:00Z"))
                    .coefficient(new Fraction(1, 2))
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(ExamResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(updated);
    assertEquals(created.id(), updated.id());
  }

  @Test
  void teacherCannotUpdateExamOfCourseTheyDoNotTeach() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var ownerTeacher = createTeacher();
    var otherTeacher = createTeacher();
    var assignment = createAssignment(course, group, ownerTeacher);
    var created = createExam(adminToken(), assignment.getId());

    webTestClient
        .put()
        .uri("/course-assignments/" + assignment.getId() + "/exams")
        .header("Authorization", "Bearer " + loginToken(otherTeacher.getEmail(), "secret123"))
        .bodyValue(
            ExamRequest.builder()
                .id(created.id())
                .courseAssignmentId(assignment.getId())
                .title("Hijack " + UUID.randomUUID())
                .examDatetime(Instant.parse("2024-06-15T09:00:00Z"))
                .coefficient(new Fraction(1, 2))
                .build())
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void teacherCanDeleteExamTheyOwn() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);
    var created = createExam(adminToken(), assignment.getId());

    webTestClient
        .delete()
        .uri("/course-assignments/" + assignment.getId() + "/exams/" + created.id())
        .header("Authorization", "Bearer " + loginToken(teacher.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isNoContent();
  }

  @Test
  void teacherCannotDeleteExamOfCourseTheyDoNotTeach() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var ownerTeacher = createTeacher();
    var otherTeacher = createTeacher();
    var assignment = createAssignment(course, group, ownerTeacher);
    var created = createExam(adminToken(), assignment.getId());

    webTestClient
        .delete()
        .uri("/course-assignments/" + assignment.getId() + "/exams/" + created.id())
        .header("Authorization", "Bearer " + loginToken(otherTeacher.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void teacherCanListExamsOfCourseTheyTeach() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);
    var token = adminToken();
    createExam(token, assignment.getId());
    createExam(token, assignment.getId());

    var exams =
        webTestClient
            .get()
            .uri("/course-assignments/" + assignment.getId() + "/exams")
            .header("Authorization", "Bearer " + loginToken(teacher.getEmail(), "secret123"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ExamResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(exams);
    assertEquals(2, exams.size());
  }

  @Test
  void teacherCannotListExamsOfCourseTheyDoNotTeach() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var ownerTeacher = createTeacher();
    var otherTeacher = createTeacher();
    var assignment = createAssignment(course, group, ownerTeacher);

    webTestClient
        .get()
        .uri("/course-assignments/" + assignment.getId() + "/exams")
        .header("Authorization", "Bearer " + loginToken(otherTeacher.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void teacherCanGetExamByIdOfCourseTheyTeach() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);
    var created = createExam(adminToken(), assignment.getId());

    var fetched =
        webTestClient
            .get()
            .uri("/course-assignments/" + assignment.getId() + "/exams/" + created.id())
            .header("Authorization", "Bearer " + loginToken(teacher.getEmail(), "secret123"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(ExamResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(fetched);
    assertEquals(created.id(), fetched.id());
  }

  @Test
  void teacherCannotGetExamByIdOfCourseTheyDoNotTeach() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var ownerTeacher = createTeacher();
    var otherTeacher = createTeacher();
    var assignment = createAssignment(course, group, ownerTeacher);
    var created = createExam(adminToken(), assignment.getId());

    webTestClient
        .get()
        .uri("/course-assignments/" + assignment.getId() + "/exams/" + created.id())
        .header("Authorization", "Bearer " + loginToken(otherTeacher.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void studentCanViewExamsOfTheirCurriculum() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);
    var student = createStudent(promotion, "student-" + UUID.randomUUID() + "@hei.school");
    joinGroup(student, group);
    var token = adminToken();
    createExam(token, assignment.getId());
    createExam(token, assignment.getId());

    var exams =
        webTestClient
            .get()
            .uri("/course-assignments/" + assignment.getId() + "/exams")
            .header("Authorization", "Bearer " + loginToken(student.getEmail(), "secret123"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(ExamResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(exams);
    assertEquals(2, exams.size());
  }

  @Test
  void studentCannotViewExamsOutsideCurriculum() {
    var promotion = createPromotion();
    var groupA = createGroup(promotion);
    var groupB = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignmentB = createAssignment(course, groupB, teacher);
    var student = createStudent(promotion, "student-" + UUID.randomUUID() + "@hei.school");
    joinGroup(student, groupA);

    webTestClient
        .get()
        .uri("/course-assignments/" + assignmentB.getId() + "/exams")
        .header("Authorization", "Bearer " + loginToken(student.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void studentCanGetExamByIdInCurriculum() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);
    var student = createStudent(promotion, "student-" + UUID.randomUUID() + "@hei.school");
    joinGroup(student, group);
    var created = createExam(adminToken(), assignment.getId());

    var fetched =
        webTestClient
            .get()
            .uri("/course-assignments/" + assignment.getId() + "/exams/" + created.id())
            .header("Authorization", "Bearer " + loginToken(student.getEmail(), "secret123"))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(ExamResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(fetched);
    assertEquals(created.id(), fetched.id());
  }

  @Test
  void studentCannotGetExamByIdOutsideCurriculum() {
    var promotion = createPromotion();
    var groupA = createGroup(promotion);
    var groupB = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignmentB = createAssignment(course, groupB, teacher);
    var student = createStudent(promotion, "student-" + UUID.randomUUID() + "@hei.school");
    joinGroup(student, groupA);
    var created = createExam(adminToken(), assignmentB.getId());

    webTestClient
        .get()
        .uri("/course-assignments/" + assignmentB.getId() + "/exams/" + created.id())
        .header("Authorization", "Bearer " + loginToken(student.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void examCoefficientSumIsRejected() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);
    var token = adminToken();
    createExamWithCoefficient(token, assignment.getId(), new Fraction(3, 4));

    webTestClient
        .put()
        .uri("/course-assignments/" + assignment.getId() + "/exams")
        .header("Authorization", "Bearer " + token)
        .bodyValue(examRequest(assignment.getId(), new Fraction(1, 2)))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void unknownCourseAssignmentIsRejected() {
    webTestClient
        .get()
        .uri("/course-assignments/" + UUID.randomUUID() + "/exams")
        .header("Authorization", "Bearer " + adminToken())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void unknownExamIdIsRejected() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);

    webTestClient
        .get()
        .uri("/course-assignments/" + assignment.getId() + "/exams/" + UUID.randomUUID())
        .header("Authorization", "Bearer " + adminToken())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void courseAssignmentPathMismatchIsRejected() {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    var courseB = createCourse();
    var teacher = createTeacher();
    var assignment = createAssignment(course, group, teacher);
    var otherAssignment = createAssignment(courseB, group, teacher);

    webTestClient
        .put()
        .uri("/course-assignments/" + assignment.getId() + "/exams")
        .header("Authorization", "Bearer " + adminToken())
        .bodyValue(examRequest(otherAssignment.getId(), new Fraction(1, 2)))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  private void createExamWithCoefficient(
      String token, UUID courseAssignmentId, Fraction coefficient) {
    webTestClient
        .put()
        .uri("/course-assignments/" + courseAssignmentId + "/exams")
        .header("Authorization", "Bearer " + token)
        .bodyValue(examRequest(courseAssignmentId, coefficient))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(ExamResponse.class)
        .returnResult();
  }

  private ExamResponse createExamWithRawCoefficient(
      String token, UUID courseAssignmentId, Object coefficient) {
    return webTestClient
        .put()
        .uri("/course-assignments/" + courseAssignmentId + "/exams")
        .header("Authorization", "Bearer " + token)
        .bodyValue(
            Map.of(
                "courseAssignmentId",
                courseAssignmentId.toString(),
                "title",
                "Raw coeff " + UUID.randomUUID(),
                "examDatetime",
                Instant.parse("2024-06-01T09:00:00Z"),
                "coefficient",
                coefficient))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(ExamResponse.class)
        .returnResult()
        .getResponseBody();
  }

  @Test
  void adminCanCreateExamWithRawCoefficientForms() {
    var token = adminToken();

    var quarter =
        createExamWithRawCoefficient(token, createAssignmentFor(createTeacher()).getId(), 0.25);
    assertNotNull(quarter);
    assertEquals(new Fraction(1, 4), quarter.coefficient());

    var half =
        createExamWithRawCoefficient(token, createAssignmentFor(createTeacher()).getId(), 0.5);
    assertNotNull(half);
    assertEquals(new Fraction(1, 2), half.coefficient());

    var third =
        createExamWithRawCoefficient(token, createAssignmentFor(createTeacher()).getId(), "1/3");
    assertNotNull(third);
    assertEquals(new Fraction(1, 3), third.coefficient());
  }

  @Test
  void teacherCanCreateExamWithRawCoefficientForms() {
    var teacher = createTeacher();
    var token = loginToken(teacher.getEmail(), "secret123");

    var quarter = createExamWithRawCoefficient(token, createAssignmentFor(teacher).getId(), 0.25);
    assertNotNull(quarter);
    assertEquals(new Fraction(1, 4), quarter.coefficient());

    var half = createExamWithRawCoefficient(token, createAssignmentFor(teacher).getId(), 0.5);
    assertNotNull(half);
    assertEquals(new Fraction(1, 2), half.coefficient());

    var third = createExamWithRawCoefficient(token, createAssignmentFor(teacher).getId(), "1/3");
    assertNotNull(third);
    assertEquals(new Fraction(1, 3), third.coefficient());
  }

  @Test
  void invalidNumericCoefficientsAreRejectedWithBadRequest() {
    var token = adminToken();
    var assignmentId = createAssignmentFor(createTeacher()).getId();

    for (Object invalid : List.of(0.0, -0.5, 1.5)) {
      webTestClient
          .put()
          .uri("/course-assignments/" + assignmentId + "/exams")
          .header("Authorization", "Bearer " + token)
          .bodyValue(
              Map.of(
                  "courseAssignmentId",
                  assignmentId.toString(),
                  "title",
                  "Invalid coeff",
                  "examDatetime",
                  Instant.parse("2024-06-01T09:00:00Z"),
                  "coefficient",
                  invalid))
          .exchange()
          .expectStatus()
          .isBadRequest();
    }
  }

  private JCourseAssignment createAssignmentFor(JTeacher teacher) {
    var promotion = createPromotion();
    var group = createGroup(promotion);
    var course = createCourse();
    return createAssignment(course, group, teacher);
  }
}
