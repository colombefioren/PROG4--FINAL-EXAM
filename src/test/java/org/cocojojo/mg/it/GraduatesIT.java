package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.GraduateExportResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.GraduateResponse;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.file.bucket.BucketComponent;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.model.enums.ResultStatus;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.CourseRepository;
import org.cocojojo.mg.repository.ExamRepository;
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
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JGroupFlow;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.repository.model.JTeacher;
import org.cocojojo.mg.service.ResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class GraduatesIT extends FacadeIT {

  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  @Autowired private AdminRepository adminRepository;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private GroupFlowRepository groupFlowRepository;
  @Autowired private CourseAssignmentRepository courseAssignmentRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private GradeRepository gradeRepository;
  @MockBean private BucketComponent bucketComponent;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ResultService resultService;

  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtService jwtService;

  @LocalServerPort int port;
  private WebTestClient webTestClient;

  @BeforeEach
  @SneakyThrows
  void setUp() {
    // A longer response timeout keeps the export test (xlsx generation + S3 upload) from
    // flaking with a blocking-read timeout when test forks share CPU under CI load.
    webTestClient =
        WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .responseTimeout(Duration.ofSeconds(30))
            .build();
    when(bucketComponent.presign(any(), any()))
        .thenReturn(
            URI.create("https://dummy-bucket.s3.eu-west-3.amazonaws.com/graduates/list.xlsx")
                .toURL());
    // Grades are soft-deleted (is_deleted), so a physical purge is needed to free the
    // grade.student_id FK before students can be removed.
    jdbcTemplate.execute("delete from \"grade_history\"");
    jdbcTemplate.execute("delete from \"grade\"");
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

  private String unique(String prefix) {
    return "grad-" + prefix + SEQUENCE.incrementAndGet() + "@hei.school";
  }

  private JAdmin saveAdmin() {
    return adminRepository.save(
        JAdmin.builder()
            .firstname("Ada")
            .lastname("Lovelace")
            .email(unique("admin"))
            .password(passwordEncoder.encode("secret123"))
            .build());
  }

  private JTeacher saveTeacher() {
    return teacherRepository.save(
        JTeacher.builder()
            .firstname("Grace")
            .lastname("Hopper")
            .email(unique("teacher"))
            .password(passwordEncoder.encode("secret123"))
            .build());
  }

  private JPromotion savePromotion() {
    return promotionRepository.save(
        JPromotion.builder()
            .ref("GRAD-PROMO" + SEQUENCE.incrementAndGet())
            .name("GRAD Promotion " + SEQUENCE.incrementAndGet())
            .entryYear(2023)
            .build());
  }

  private JGroup saveGroup(JPromotion promotion, Track track) {
    return groupRepository.save(
        JGroup.builder()
            .promotion(promotion)
            .ref("GRAD-GRP" + SEQUENCE.incrementAndGet())
            .track(track)
            .build());
  }

  private JStudent saveStudent(JPromotion promotion, JGroup group) {
    var student =
        studentRepository.save(
            JStudent.builder()
                .firstname("Alan")
                .lastname("Turing")
                .email(unique("student"))
                .password(passwordEncoder.encode("secret123"))
                .std("GRAD-STD" + SEQUENCE.incrementAndGet())
                .promotion(promotion)
                .build());
    groupFlowRepository.save(
        JGroupFlow.builder()
            .student(student)
            .group(group)
            .groupFlowType(GroupFlowType.JOIN)
            .build());
    return student;
  }

  private JCourse saveCourse(StudentLevel level, Track track) {
    return courseRepository.save(
        JCourse.builder()
            .code("GRAD-CODE" + SEQUENCE.incrementAndGet())
            .name("Course " + SEQUENCE.incrementAndGet())
            .credits(4)
            .totalHours(30)
            .studentLevel(level)
            .track(track)
            .build());
  }

  private JCourseAssignment saveAssignment(JCourse course, JGroup group, Semester semester) {
    return courseAssignmentRepository.save(
        JCourseAssignment.builder()
            .course(course)
            .group(group)
            .teachers(List.of(saveTeacher()))
            .academicYear(2024)
            .semester(semester)
            .credits(course.getCredits())
            .build());
  }

  @Test
  void courseWithPartialCoefficientWeightIsNotComplete() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var student = saveStudent(promotion, group);
    var course = saveCourse(StudentLevel.L1, null);
    var assignment = saveAssignment(course, group, Semester.S1);
    var quarterExam =
        examRepository.save(
            JExam.builder()
                .courseAssignment(assignment)
                .title("Q1 " + SEQUENCE.incrementAndGet())
                .examDatetime(Instant.parse("2025-06-01T08:00:00Z"))
                .coefficientNumerator(1)
                .coefficientDenominator(4)
                .build());
    saveGrade(quarterExam, student, new BigDecimal("16"));

    var summary = resultService.computeResultsSummary(student.getId());
    var l1 =
        summary.levels().stream()
            .filter(l -> l.level() == StudentLevel.L1)
            .findFirst()
            .orElseThrow();
    // Only a quarter of the course weight has been scheduled and graded: not complete.
    assertEquals(ResultStatus.PROVISIONAL, l1.status());
    assertFalse(l1.courses().get(0).complete().booleanValue());
    assertFalse(l1.courses().get(0).passed().booleanValue());

    // ... and the student is therefore not a graduate.
    var graduates = getGraduates(token(admin), promotion.getId());
    assertTrue(graduates.isEmpty());
  }

  @Test
  void courseIsCompleteOnceGradedCoefficientsSumToOne() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var student = saveStudent(promotion, group);
    var course = saveCourse(StudentLevel.L1, null);
    var assignment = saveAssignment(course, group, Semester.S1);
    var quarter =
        examRepository.save(
            JExam.builder()
                .courseAssignment(assignment)
                .title("Q " + SEQUENCE.incrementAndGet())
                .examDatetime(Instant.parse("2025-06-01T08:00:00Z"))
                .coefficientNumerator(1)
                .coefficientDenominator(4)
                .build());
    var half =
        examRepository.save(
            JExam.builder()
                .courseAssignment(assignment)
                .title("H " + SEQUENCE.incrementAndGet())
                .examDatetime(Instant.parse("2025-06-01T09:00:00Z"))
                .coefficientNumerator(1)
                .coefficientDenominator(2)
                .build());
    var lastQuarter =
        examRepository.save(
            JExam.builder()
                .courseAssignment(assignment)
                .title("L " + SEQUENCE.incrementAndGet())
                .examDatetime(Instant.parse("2025-06-01T10:00:00Z"))
                .coefficientNumerator(1)
                .coefficientDenominator(4)
                .build());
    saveGrade(quarter, student, new BigDecimal("16"));
    saveGrade(half, student, new BigDecimal("14"));

    // Half of the weight graded: still incomplete even though two of three exams are graded.
    var partial = resultService.computeResultsSummary(student.getId());
    var l1Partial =
        partial.levels().stream()
            .filter(l -> l.level() == StudentLevel.L1)
            .findFirst()
            .orElseThrow();
    assertEquals(ResultStatus.PROVISIONAL, l1Partial.status());

    saveGrade(lastQuarter, student, new BigDecimal("12"));
    var complete = resultService.computeResultsSummary(student.getId());
    var l1Complete =
        complete.levels().stream()
            .filter(l -> l.level() == StudentLevel.L1)
            .findFirst()
            .orElseThrow();
    assertEquals(ResultStatus.COMPLETED, l1Complete.status());
    assertTrue(l1Complete.courses().get(0).passed().booleanValue());

    // Only L1 exists in this curriculum, so the student cannot be a graduate yet — the point
    // is that the course itself is complete.
    var graduates = getGraduates(token(admin), promotion.getId());
    assertTrue(graduates.isEmpty());
  }

  @Test
  void singleFullCoefficientExamStillCompletesTheCourse() {
    // Regression: the standard one 1/1-exam-per-course curriculum (used by the other fixtures)
    // must still complete once graded under the new coefficient rule.
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var student = saveStudent(promotion, group);
    var course = saveCourse(StudentLevel.L1, null);
    var assignment = saveAssignment(course, group, Semester.S1);
    var exam =
        examRepository.save(
            JExam.builder()
                .courseAssignment(assignment)
                .title("Full " + SEQUENCE.incrementAndGet())
                .examDatetime(Instant.parse("2025-06-01T08:00:00Z"))
                .coefficientNumerator(1)
                .coefficientDenominator(1)
                .build());
    saveGrade(exam, student, new BigDecimal("14"));

    var summary = resultService.computeResultsSummary(student.getId());
    var l1 =
        summary.levels().stream()
            .filter(l -> l.level() == StudentLevel.L1)
            .findFirst()
            .orElseThrow();
    assertEquals(ResultStatus.COMPLETED, l1.status());
    assertTrue(l1.courses().get(0).complete().booleanValue());
    assertTrue(l1.courses().get(0).passed().booleanValue());
  }

  private JExam saveExam(JCourseAssignment assignment) {
    return examRepository.save(
        JExam.builder()
            .courseAssignment(assignment)
            .title("Exam " + SEQUENCE.incrementAndGet())
            .examDatetime(Instant.parse("2025-06-01T08:00:00Z"))
            .coefficientNumerator(1)
            .coefficientDenominator(1)
            .build());
  }

  private void saveGrade(JExam exam, JStudent student, BigDecimal value) {
    gradeRepository.save(JGrade.builder().exam(exam).student(student).value(value).build());
  }

  private String token(JAdmin admin) {
    return jwtService.generateToken(admin.getId(), admin.getEmail(), Role.ADMIN);
  }

  private String token(JTeacher teacher) {
    return jwtService.generateToken(teacher.getId(), teacher.getEmail(), Role.TEACHER);
  }

  private String token(JStudent student) {
    return jwtService.generateToken(student.getId(), student.getEmail(), Role.STUDENT);
  }

  private List<GraduateResponse> getGraduates(String token, UUID promotionId) {
    var body =
        webTestClient
            .get()
            .uri("/promotions/{promotionId}/graduates", promotionId)
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(GraduateResponse.class)
            .returnResult()
            .getResponseBody();
    assertNotNull(body);
    return body;
  }

  /** Returns the three exams of the shared curriculum (one course per level) for the group. */
  private List<JExam> createCurriculum(JGroup group) {
    var courses =
        List.of(
            saveCourse(StudentLevel.L1, null),
            saveCourse(StudentLevel.L2, group.getTrack()),
            saveCourse(StudentLevel.L3, group.getTrack()));
    var semesters = List.of(Semester.S1, Semester.S3, Semester.S5);
    return java.util.stream.IntStream.range(0, courses.size())
        .mapToObj(i -> saveExam(saveAssignment(courses.get(i), group, semesters.get(i))))
        .toList();
  }

  @Test
  void adminCanListGraduatesRankedByDescendingAverage() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var topStudent = saveStudent(promotion, group);
    var lowerStudent = saveStudent(promotion, group);
    var exams = createCurriculum(group);
    saveGrade(exams.get(0), topStudent, new BigDecimal("16"));
    saveGrade(exams.get(1), topStudent, new BigDecimal("15"));
    saveGrade(exams.get(2), topStudent, new BigDecimal("17"));
    saveGrade(exams.get(0), lowerStudent, new BigDecimal("11"));
    saveGrade(exams.get(1), lowerStudent, new BigDecimal("12"));
    saveGrade(exams.get(2), lowerStudent, new BigDecimal("13"));

    var graduates = getGraduates(token(admin), promotion.getId());

    assertEquals(2, graduates.size());
    assertEquals(1, graduates.get(0).rank());
    assertEquals(topStudent.getStd(), graduates.get(0).std());
    assertEquals(2, graduates.get(1).rank());
    assertEquals(lowerStudent.getStd(), graduates.get(1).std());
    assertTrue(graduates.get(0).generalAverage().compareTo(graduates.get(1).generalAverage()) > 0);
  }

  @Test
  void failingStudentIsNotAGraduate() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var passingStudent = saveStudent(promotion, group);
    var failingStudent = saveStudent(promotion, group);
    var exams = createCurriculum(group);
    saveGrade(exams.get(0), passingStudent, new BigDecimal("14"));
    saveGrade(exams.get(1), passingStudent, new BigDecimal("15"));
    saveGrade(exams.get(2), passingStudent, new BigDecimal("16"));
    saveGrade(exams.get(0), failingStudent, new BigDecimal("12"));
    saveGrade(exams.get(1), failingStudent, new BigDecimal("8"));
    saveGrade(exams.get(2), failingStudent, new BigDecimal("13"));

    var graduates = getGraduates(token(admin), promotion.getId());

    assertEquals(1, graduates.size());
    assertEquals(passingStudent.getStd(), graduates.get(0).std());
  }

  @Test
  void onlyStudentsOfThePromotionAreListed() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var otherPromotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var otherGroup = saveGroup(otherPromotion, Track.TN);
    var student = saveStudent(promotion, group);
    saveStudent(otherPromotion, otherGroup);
    var exams = createCurriculum(group);
    saveGrade(exams.get(0), student, new BigDecimal("14"));
    saveGrade(exams.get(1), student, new BigDecimal("15"));
    saveGrade(exams.get(2), student, new BigDecimal("16"));

    var graduates = getGraduates(token(admin), promotion.getId());

    assertEquals(1, graduates.size());
    assertEquals(student.getStd(), graduates.get(0).std());
  }

  @Test
  void teacherCannotAccessGraduates() {
    var teacher = saveTeacher();
    var promotion = savePromotion();

    webTestClient
        .get()
        .uri("/promotions/{promotionId}/graduates", promotion.getId())
        .header("Authorization", "Bearer " + token(teacher))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void studentCannotAccessGraduates() {
    var student = saveStudent(savePromotion(), saveGroup(savePromotion(), Track.TN));

    webTestClient
        .get()
        .uri("/promotions/{promotionId}/graduates", student.getPromotion().getId())
        .header("Authorization", "Bearer " + token(student))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void unauthenticatedCannotAccessGraduates() {
    webTestClient
        .get()
        .uri("/promotions/{promotionId}/graduates", UUID.randomUUID())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void unknownPromotionReturnsNotFound() {
    var admin = saveAdmin();

    webTestClient
        .get()
        .uri("/promotions/{promotionId}/graduates", UUID.randomUUID())
        .header("Authorization", "Bearer " + token(admin))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void adminCanExportGraduateListAsPresignedUrl() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var student = saveStudent(promotion, group);
    var exams = createCurriculum(group);
    saveGrade(exams.get(0), student, new BigDecimal("14"));
    saveGrade(exams.get(1), student, new BigDecimal("15"));
    saveGrade(exams.get(2), student, new BigDecimal("16"));

    var body =
        webTestClient
            .get()
            .uri("/promotions/{promotionId}/graduates/export", promotion.getId())
            .header("Authorization", "Bearer " + token(admin))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(GraduateExportResponse.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(body);
    assertNotNull(body.url());
    assertTrue(body.url().startsWith("https://dummy-bucket"));
  }

  @Test
  void teacherCannotExportGraduateList() {
    var teacher = saveTeacher();
    var promotion = savePromotion();

    webTestClient
        .get()
        .uri("/promotions/{promotionId}/graduates/export", promotion.getId())
        .header("Authorization", "Bearer " + token(teacher))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void unauthenticatedCannotExportGraduateList() {
    webTestClient
        .get()
        .uri("/promotions/{promotionId}/graduates/export", UUID.randomUUID())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  @SneakyThrows
  void adminCanDownloadGraduateListAsAttachment() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var student = saveStudent(promotion, group);
    var exams = createCurriculum(group);
    saveGrade(exams.get(0), student, new BigDecimal("14"));
    saveGrade(exams.get(1), student, new BigDecimal("15"));
    saveGrade(exams.get(2), student, new BigDecimal("16"));

    var bytes =
        webTestClient
            .get()
            .uri("/promotions/{promotionId}/graduates/download", promotion.getId())
            .header("Authorization", "Bearer " + token(admin))
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .expectHeader()
            .value(
                "Content-Disposition",
                value -> {
                  assertTrue(value.startsWith("attachment; filename=\""));
                  assertTrue(value.contains(promotion.getRef()));
                  assertTrue(value.contains(" - GRADUATE LIST - "));
                  assertTrue(value.endsWith(".xlsx\""));
                })
            .expectBody(byte[].class)
            .returnResult()
            .getResponseBody();

    assertNotNull(bytes);
    assertTrue(bytes.length > 0);
    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      var sheet = workbook.getSheet("Diplomes");
      assertNotNull(sheet);
      assertEquals("Rang", sheet.getRow(0).getCell(0).getStringCellValue());
    }
  }

  @Test
  void teacherCannotDownloadGraduateList() {
    var teacher = saveTeacher();
    var promotion = savePromotion();

    webTestClient
        .get()
        .uri("/promotions/{promotionId}/graduates/download", promotion.getId())
        .header("Authorization", "Bearer " + token(teacher))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void unauthenticatedCannotDownloadGraduateList() {
    webTestClient
        .get()
        .uri("/promotions/{promotionId}/graduates/download", UUID.randomUUID())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void adminCanSeePromotionsUiPage() {
    var admin = saveAdmin();
    var promotion = savePromotion();

    var body =
        webTestClient
            .get()
            .uri("/ui/promotions")
            .header("Authorization", "Bearer " + token(admin))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertNotNull(body);
    assertTrue(body.contains(promotion.getRef()));
    assertTrue(body.contains("Download Graduate List"));
    assertTrue(body.contains("data-promotion-id=\"" + promotion.getId() + "\""));
  }

  @Test
  void studentWhoChangedGroupKeepsLatestTrack() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var tnGroup = saveGroup(promotion, Track.TN);
    var elGroup = saveGroup(promotion, Track.EL);
    var student = saveStudent(promotion, tnGroup);
    groupFlowRepository.save(
        JGroupFlow.builder()
            .student(student)
            .group(elGroup)
            .groupFlowType(GroupFlowType.JOIN)
            .build());

    // Courses from BOTH groups the student passed through count toward the curriculum,
    // while the reported track follows the most recently joined group (EL).
    var tnExams = createCurriculum(tnGroup);
    var elExams = createCurriculum(elGroup);
    saveGrade(tnExams.get(0), student, new BigDecimal("14"));
    saveGrade(tnExams.get(1), student, new BigDecimal("15"));
    saveGrade(tnExams.get(2), student, new BigDecimal("16"));
    saveGrade(elExams.get(0), student, new BigDecimal("14"));
    saveGrade(elExams.get(1), student, new BigDecimal("15"));
    saveGrade(elExams.get(2), student, new BigDecimal("16"));

    var graduates = getGraduates(token(admin), promotion.getId());

    assertEquals(1, graduates.size());
    assertEquals(student.getStd(), graduates.get(0).std());
    assertEquals(Track.EL, graduates.get(0).track());
  }

  @Test
  void catalogCourseNeverAssignedDoesNotBlockGraduation() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var student = saveStudent(promotion, group);
    var assignedL1 = saveCourse(StudentLevel.L1, null);
    var assignedL2 = saveCourse(StudentLevel.L2, Track.TN);
    var assignedL3 = saveCourse(StudentLevel.L3, Track.TN);
    // A catalog course at L2 that the promotion never assigns (course substitution).
    saveCourse(StudentLevel.L2, Track.TN);

    var exams =
        List.of(
            saveExam(saveAssignment(assignedL1, group, Semester.S1)),
            saveExam(saveAssignment(assignedL2, group, Semester.S3)),
            saveExam(saveAssignment(assignedL3, group, Semester.S5)));
    saveGrade(exams.get(0), student, new BigDecimal("14"));
    saveGrade(exams.get(1), student, new BigDecimal("15"));
    saveGrade(exams.get(2), student, new BigDecimal("16"));

    var graduates = getGraduates(token(admin), promotion.getId());

    assertEquals(1, graduates.size());
    assertEquals(student.getStd(), graduates.get(0).std());
  }

  @Test
  void teacherCannotSeePromotionsUiPage() {
    var teacher = saveTeacher();

    webTestClient
        .get()
        .uri("/ui/promotions")
        .header("Authorization", "Bearer " + token(teacher))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void adminCanSeePromotionsUiPageWithBasicAuth() {
    var admin = saveAdmin();
    var promotion = savePromotion();

    webTestClient
        .get()
        .uri("/ui/promotions")
        .headers(headers -> headers.setBasicAuth(admin.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .consumeWith(
            body -> {
              var html = new String(body.getResponseBody());
              assertTrue(html.contains(promotion.getRef()));
              assertTrue(html.contains("Download Graduate List"));
            });
  }

  @Test
  void uiPagePromptsForBasicAuthWhenUnauthenticated() {
    webTestClient
        .get()
        .uri("/ui/promotions")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .valueEquals("WWW-Authenticate", "Basic realm=\"hei\"");
  }

  @Test
  void staticCssIsServedWithoutAuthentication() {
    webTestClient
        .get()
        .uri("/css/promotions.css")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.valueOf("text/css"))
        .expectBody(String.class)
        .consumeWith(
            body -> assertTrue(new String(body.getResponseBody()).contains("font-family")));
  }

  @Test
  void teacherCannotSeePromotionsUiPageWithBasicAuth() {
    var teacher = saveTeacher();

    webTestClient
        .get()
        .uri("/ui/promotions")
        .headers(headers -> headers.setBasicAuth(teacher.getEmail(), "secret123"))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void unauthenticatedCannotSeePromotionsUiPage() {
    webTestClient.get().uri("/ui/promotions").exchange().expectStatus().isUnauthorized();
  }
}
