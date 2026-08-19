package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupFlowResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.MoveStudentGroupRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.StudentRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.StudentResponse;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.CourseRepository;
import org.cocojojo.mg.repository.ExamRepository;
import org.cocojojo.mg.repository.GradeHistoryRepository;
import org.cocojojo.mg.repository.GradeRepository;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.GroupRepository;
import org.cocojojo.mg.repository.PromotionRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JExam;
import org.cocojojo.mg.repository.model.JGrade;
import org.cocojojo.mg.repository.model.JGradeHistory;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JTeacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class StudentIT extends FacadeIT {

  @Autowired private AdminRepository adminRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtService jwtService;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private CourseAssignmentRepository courseAssignmentRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private GradeRepository gradeRepository;
  @Autowired private GradeHistoryRepository gradeHistoryRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private GroupFlowRepository groupFlowRepository;

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

  private String studentToken(UUID studentId, String email) {
    return jwtService.generateToken(studentId, email, Role.STUDENT);
  }

  private JPromotion savePromotion() {
    return promotionRepository.save(
        JPromotion.builder()
            .ref("SI-PROMO-" + UUID.randomUUID().toString().substring(0, 8))
            .name("SI Promotion")
            .entryYear(2025)
            .build());
  }

  private JTeacher saveTeacher() {
    return teacherRepository.save(
        JTeacher.builder()
            .firstname("Grace")
            .lastname("Hopper")
            .email("si-teacher-" + UUID.randomUUID().toString().substring(0, 8) + "@hei.school")
            .password(passwordEncoder.encode("secret123"))
            .build());
  }

  private JCourse saveCourse() {
    return courseRepository.save(
        JCourse.builder()
            .code("SI-C-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .name("SI Course")
            .credits(4)
            .studentLevel(StudentLevel.L3)
            .build());
  }

  private JGroup saveGroup() {
    return groupRepository.save(
        JGroup.builder()
            .promotion(savePromotion())
            .ref("SI-G-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .track(Track.TN)
            .build());
  }

  private JExam saveExam(JCourseAssignment assignment) {
    return examRepository.save(
        JExam.builder()
            .courseAssignment(assignment)
            .title("SI Exam")
            .examDatetime(Instant.parse("2026-06-01T08:00:00Z"))
            .coefficientNumerator(1)
            .coefficientDenominator(1)
            .build());
  }

  private JCourseAssignment saveAssignment(JCourse course, JGroup group, JTeacher teacher) {
    return courseAssignmentRepository.save(
        JCourseAssignment.builder()
            .course(course)
            .group(group)
            .teachers(List.of(teacher))
            .academicYear(2025)
            .semester(Semester.S1)
            .credits(4)
            .build());
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
  void concurrentCreationGeneratesUniqueSequentialStds() {
    var promotion = createPromotion(2025);
    var group = createGroup(promotion.id());
    var seed = createStudent(group.id(), uniqueEmail(), "password123");
    int seedSeq = Integer.parseInt(seed.std().substring(5));

    int threads = 5;
    var executor = Executors.newFixedThreadPool(threads);
    try {
      var stds =
          java.util.stream.IntStream.range(0, threads)
              .mapToObj(
                  i ->
                      CompletableFuture.supplyAsync(
                          () -> createStudent(group.id(), uniqueEmail(), "password123"), executor))
              .map(f -> f.join().std())
              .toList();

      assertEquals(threads, stds.stream().distinct().count());
      assertTrue(stds.stream().allMatch(s -> s.startsWith("STD25")));
      var expected = new HashSet<String>();
      for (int i = 1; i <= threads; i++) {
        expected.add("STD25" + String.format("%03d", seedSeq + i));
      }
      assertEquals(expected, new HashSet<>(stds));
    } finally {
      executor.shutdownNow();
    }
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
        .uri("/students/" + student.id() + "/group-flows")
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
        .uri("/students/" + student.id() + "/group-flows")
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
        .uri("/students/" + student.id() + "/group-flows")
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
  void adminCanListStudentsPaginated() {
    var promotion = createPromotion(2024);
    var group = createGroup(promotion.id());
    var email = uniqueEmail();
    var student = createStudent(group.id(), email, "password123");

    var page =
        webTestClient
            .get()
            .uri("/students?page=0&size=10")
            .header("Authorization", "Bearer " + adminToken())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new ParameterizedTypeReference<TestPage<StudentResponse>>() {})
            .returnResult()
            .getResponseBody();

    assertNotNull(page);
    assertTrue(page.totalElements() >= 1);
    assertTrue(page.content().stream().anyMatch(s -> s.id().equals(student.id())));
  }

  @Test
  void adminCanReadStudentGroupFlowHistory() {
    var promotion = createPromotion(2024);
    var group = createGroup(promotion.id());
    var student = createStudent(group.id(), uniqueEmail(), "password123");

    var flows =
        webTestClient
            .get()
            .uri("/students/" + student.id() + "/group-flows")
            .header("Authorization", "Bearer " + adminToken())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new ParameterizedTypeReference<List<GroupFlowResponse>>() {})
            .returnResult()
            .getResponseBody();

    assertNotNull(flows);
    assertTrue(flows.stream().anyMatch(f -> f.groupId().equals(group.id())));
    assertTrue(flows.stream().anyMatch(f -> f.studentId().equals(student.id())));
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

  @Test
  void adminCanDeleteAStudent() {
    var promotion = createPromotion(2025);
    var group = createGroup(promotion.id());
    var email = "to-delete-" + UUID.randomUUID().toString().substring(0, 8) + "@hei.school";
    var student = createStudent(group.id(), email, "secret123");

    webTestClient
        .delete()
        .uri("/students/" + student.id())
        .header("Authorization", "Bearer " + adminToken())
        .exchange()
        .expectStatus()
        .isNoContent();

    webTestClient
        .get()
        .uri("/students/" + student.id())
        .header("Authorization", "Bearer " + adminToken())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void deletingAnUnknownStudentReturnsNotFound() {
    webTestClient
        .delete()
        .uri("/students/" + UUID.randomUUID())
        .header("Authorization", "Bearer " + adminToken())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void studentCannotDeleteAStudent() {
    var promotion = createPromotion(2025);
    var group = createGroup(promotion.id());
    var email = "victim-" + UUID.randomUUID().toString().substring(0, 8) + "@hei.school";
    var student = createStudent(group.id(), email, "secret123");

    webTestClient
        .delete()
        .uri("/students/" + student.id())
        .header("Authorization", "Bearer " + studentToken(student.id(), email))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void deletingAStudentCascadesGradesGroupFlowsAndHistories() {
    var promotion = createPromotion(2025);
    var group = createGroup(promotion.id());
    var email = "cascade-" + UUID.randomUUID().toString().substring(0, 8) + "@hei.school";
    var student = createStudent(group.id(), email, "secret123");

    var course = saveCourse();
    var teacher = saveTeacher();
    var exam = saveExam(saveAssignment(course, saveGroup(), teacher));
    var jStudent = studentRepository.getReferenceById(student.id());
    var grade =
        gradeRepository.save(
            JGrade.builder()
                .exam(exam)
                .student(jStudent)
                .value(new BigDecimal("12.50"))
                .comment("ok")
                .build());
    var history =
        gradeHistoryRepository.save(
            JGradeHistory.builder()
                .grade(grade)
                .previousValue(new BigDecimal("10.00"))
                .newValue(new BigDecimal("12.50"))
                .reason("correction")
                .changedBy(jStudent)
                .build());

    webTestClient
        .delete()
        .uri("/students/" + student.id())
        .header("Authorization", "Bearer " + adminToken())
        .exchange()
        .expectStatus()
        .isNoContent();

    assertTrue(gradeHistoryRepository.findById(history.getId()).isEmpty());
    assertTrue(gradeRepository.findById(grade.getId()).isEmpty());
    assertTrue(groupFlowRepository.findByStudentId(student.id()).isEmpty());
  }

  private String uniqueEmail() {
    return "student-" + UUID.randomUUID().toString().substring(0, 8) + "@hei.school";
  }
}
