package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.YearlyResultResponse;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.model.enums.GroupFlowType;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class YearlyResultsIT extends FacadeIT {

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
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtService jwtService;

  @LocalServerPort int port;
  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient =
        WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .responseTimeout(Duration.ofSeconds(30))
            .build();
  }

  @AfterEach
  void tearDown() {
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
    return "yr-" + prefix + SEQUENCE.incrementAndGet() + "@hei.school";
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
            .ref("YR-PROMO" + SEQUENCE.incrementAndGet())
            .name("YR Promotion " + SEQUENCE.incrementAndGet())
            .entryYear(2023)
            .build());
  }

  private JGroup saveGroup(JPromotion promotion) {
    return groupRepository.save(
        JGroup.builder()
            .promotion(promotion)
            .ref("YR-GRP" + SEQUENCE.incrementAndGet())
            .track(Track.TN)
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
                .std("YR-STD" + SEQUENCE.incrementAndGet())
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

  private JCourse saveCourse(int credits) {
    return courseRepository.save(
        JCourse.builder()
            .code("YR-CODE" + SEQUENCE.incrementAndGet())
            .name("Course " + SEQUENCE.incrementAndGet())
            .credits(credits)
            .totalHours(30)
            .studentLevel(StudentLevel.L1)
            .track(Track.TN)
            .build());
  }

  private JCourseAssignment saveAssignment(JCourse course, JGroup group, int credits) {
    return saveAssignment(course, group, 2023, credits);
  }

  private JCourseAssignment saveAssignment(
      JCourse course, JGroup group, int academicYear, int credits) {
    return courseAssignmentRepository.save(
        JCourseAssignment.builder()
            .course(course)
            .group(group)
            .teachers(List.of(saveTeacher()))
            .academicYear(academicYear)
            .semester(Semester.S1)
            .credits(credits)
            .build());
  }

  private JExam saveExam(JCourseAssignment assignment, int numerator, int denominator) {
    return examRepository.save(
        JExam.builder()
            .courseAssignment(assignment)
            .title("Exam " + SEQUENCE.incrementAndGet())
            .examDatetime(Instant.parse("2024-06-01T08:00:00Z"))
            .coefficientNumerator(numerator)
            .coefficientDenominator(denominator)
            .build());
  }

  private JGrade saveGrade(JExam exam, JStudent student, BigDecimal value) {
    return gradeRepository.save(
        JGrade.builder().exam(exam).student(student).value(value).comment("ok").build());
  }

  private String token(JAdmin admin) {
    return jwtService.generateToken(admin.getId(), admin.getEmail(), Role.ADMIN);
  }

  private String token(JStudent student) {
    return jwtService.generateToken(student.getId(), student.getEmail(), Role.STUDENT);
  }

  private String token(JTeacher teacher) {
    return jwtService.generateToken(teacher.getId(), teacher.getEmail(), Role.TEACHER);
  }

  private YearlyResultResponse getResult(String token, UUID studentId, String level) {
    return webTestClient
        .get()
        .uri("/students/{id}/yearly_results/{level}", studentId, level)
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(YearlyResultResponse.class)
        .returnResult()
        .getResponseBody();
  }

  @Test
  void adminCanReadAnyStudentYearlyResult() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    var result = getResult(token(admin), student.getId(), "L1");

    assertEquals(StudentLevel.L1, result.level());
    assertTrue(result.courses().isEmpty());
    assertNull(result.overallAverage());
    assertEquals(0, result.earnedCredits());
    assertEquals(0, result.totalCredits());
    assertFalse(result.complete());
  }

  @Test
  void studentCanReadOwnYearlyResult() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    var result = getResult(token(student), student.getId(), "L2");

    assertEquals(StudentLevel.L2, result.level());
  }

  @Test
  void studentCannotReadAnotherStudentYearlyResult() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var other = saveStudent(promotion, group);
    var requester = saveStudent(promotion, group);

    webTestClient
        .get()
        .uri("/students/{id}/yearly_results/{level}", other.getId(), "L1")
        .header("Authorization", "Bearer " + token(requester))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void teacherCannotReadStudentYearlyResult() {
    var teacher = saveTeacher();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    webTestClient
        .get()
        .uri("/students/{id}/yearly_results/{level}", student.getId(), "L1")
        .header("Authorization", "Bearer " + token(teacher))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void anonymousCannotReadYearlyResult() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    webTestClient
        .get()
        .uri("/students/{id}/yearly_results/{level}", student.getId(), "L1")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void unknownStudentIsNotFound() {
    var admin = saveAdmin();

    webTestClient
        .get()
        .uri("/students/{id}/yearly_results/{level}", UUID.randomUUID(), "L1")
        .header("Authorization", "Bearer " + token(admin))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void invalidLevelReturnsBadRequest() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    webTestClient
        .get()
        .uri("/students/{id}/yearly_results/{level}", student.getId(), "L4")
        .header("Authorization", "Bearer " + token(admin))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void gradedCourseProducesAverageCreditsAndCompleteStatus() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);
    var course = saveCourse(4);
    var exam = saveExam(saveAssignment(course, group, 4), 1, 1);
    saveGrade(exam, student, new BigDecimal("14"));

    var result = getResult(token(admin), student.getId(), "L1");

    assertEquals(1, result.courses().size());
    var courseResult = result.courses().get(0);
    assertEquals(course.getId(), courseResult.courseId());
    assertEquals(new BigDecimal("14.00"), courseResult.average());
    assertTrue(courseResult.graded());
    assertTrue(courseResult.complete());
    assertTrue(courseResult.passed());
    assertEquals(new BigDecimal("14.00"), result.overallAverage());
    assertEquals(4, result.earnedCredits());
    assertEquals(4, result.totalCredits());
    assertTrue(result.complete());
  }

  @Test
  void failingCourseMarksLevelNotComplete() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);
    var course = saveCourse(4);
    var exam = saveExam(saveAssignment(course, group, 4), 1, 1);
    saveGrade(exam, student, new BigDecimal("8"));

    var result = getResult(token(admin), student.getId(), "L1");

    assertEquals(new BigDecimal("8.00"), result.courses().get(0).average());
    assertFalse(result.courses().get(0).passed());
    assertEquals(0, result.earnedCredits());
    assertEquals(4, result.totalCredits());
    assertFalse(result.complete());
  }

  @Test
  void overallAverageIsCreditWeightedAcrossCourses() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    var heavy = saveCourse(4);
    var heavyExam = saveExam(saveAssignment(heavy, group, 4), 1, 1);
    saveGrade(heavyExam, student, new BigDecimal("14"));

    var light = saveCourse(2);
    var lightExam = saveExam(saveAssignment(light, group, 2), 1, 1);
    saveGrade(lightExam, student, new BigDecimal("10"));

    var result = getResult(token(admin), student.getId(), "L1");

    assertEquals(2, result.courses().size());
    assertEquals(new BigDecimal("12.67"), result.overallAverage());
    assertEquals(6, result.earnedCredits());
    assertEquals(6, result.totalCredits());
    assertTrue(result.complete());
  }

  @Test
  void levelFiltersOutOtherLevelCourses() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);
    var course = saveCourse(4);
    var exam = saveExam(saveAssignment(course, group, 4), 1, 1);
    saveGrade(exam, student, new BigDecimal("14"));

    var result = getResult(token(admin), student.getId(), "L2");

    assertEquals(StudentLevel.L2, result.level());
    assertTrue(result.courses().isEmpty());
    assertNull(result.overallAverage());
    assertFalse(result.complete());
  }

  @Test
  void coursesOfOtherGroupsAreExcluded() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var otherGroup = saveGroup(promotion);
    var student = saveStudent(promotion, group);
    var course = saveCourse(4);
    var exam = saveExam(saveAssignment(course, otherGroup, 4), 1, 1);
    saveGrade(exam, student, new BigDecimal("14"));

    var result = getResult(token(admin), student.getId(), "L1");

    assertTrue(result.courses().isEmpty());
    assertEquals(0, result.totalCredits());
  }

  @Test
  void assignmentCreditsOverrideCatalogCredits() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);
    var course = saveCourse(4);
    var exam = saveExam(saveAssignment(course, group, 3), 1, 1);
    saveGrade(exam, student, new BigDecimal("14"));

    var result = getResult(token(admin), student.getId(), "L1");

    assertEquals(3, result.courses().get(0).credits());
    assertEquals(3, result.earnedCredits());
    assertEquals(3, result.totalCredits());
  }

  @Test
  void partiallyGradedCourseIsNotComplete() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);
    var course = saveCourse(4);
    var assignment = saveAssignment(course, group, 4);
    var graded = saveExam(assignment, 1, 2);
    saveGrade(graded, student, new BigDecimal("14"));
    saveExam(assignment, 1, 2);

    var result = getResult(token(admin), student.getId(), "L1");

    var courseResult = result.courses().get(0);
    assertEquals(new BigDecimal("14.00"), courseResult.average());
    assertTrue(courseResult.graded());
    assertFalse(courseResult.complete());
    assertFalse(courseResult.passed());
    assertEquals(0, result.earnedCredits());
    assertFalse(result.complete());
  }

  @Test
  void ungradedCourseHasNullAverage() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);
    var course = saveCourse(4);
    saveAssignment(course, group, 4);

    var result = getResult(token(admin), student.getId(), "L1");

    var courseResult = result.courses().get(0);
    assertNull(courseResult.average());
    assertFalse(courseResult.graded());
    assertFalse(courseResult.complete());
    assertNull(courseResult.passed());
    assertNull(result.overallAverage());
    assertEquals(0, result.earnedCredits());
    assertEquals(4, result.totalCredits());
    assertFalse(result.complete());
  }

  @Test
  void gradesFromPreviousGroupStillCount() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var oldGroup = saveGroup(promotion);
    var newGroup = saveGroup(promotion);
    var student = saveStudent(promotion, oldGroup);
    var course = saveCourse(4);
    var exam = saveExam(saveAssignment(course, oldGroup, 4), 1, 1);
    saveGrade(exam, student, new BigDecimal("14"));

    groupFlowRepository.save(
        JGroupFlow.builder()
            .student(student)
            .group(newGroup)
            .groupFlowType(GroupFlowType.JOIN)
            .build());

    var result = getResult(token(admin), student.getId(), "L1");

    assertEquals(1, result.courses().size());
    assertEquals(new BigDecimal("14.00"), result.overallAverage());
    assertTrue(result.complete());
  }

  @Test
  void overlappingAssignmentsWithCoefficientsSummingAboveOneDoNotFail() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var oldGroup = saveGroup(promotion);
    var newGroup = saveGroup(promotion);
    var student = saveStudent(promotion, oldGroup);
    var course = saveCourse(4);
    // Two assignments for the same course in groups the student passed through; grading both
    // yields coefficients summing to 3/2 > 1. This used to blow up mid-GET (Fraction::plus
    // throws on sums above 1) instead of returning the results.
    var oldExam = saveExam(saveAssignment(course, oldGroup, 2023, 4), 1, 2);
    saveGrade(oldExam, student, new BigDecimal("10"));
    var newExam = saveExam(saveAssignment(course, newGroup, 2024, 4), 1, 1);
    saveGrade(newExam, student, new BigDecimal("14"));

    groupFlowRepository.save(
        JGroupFlow.builder()
            .student(student)
            .group(newGroup)
            .groupFlowType(GroupFlowType.JOIN)
            .build());

    var result = getResult(token(admin), student.getId(), "L1");

    var courseResult = result.courses().get(0);
    assertTrue(courseResult.graded());
    // (10 * 0.5 + 14 * 1) / 1.5 = 12.67
    assertEquals(new BigDecimal("12.67"), courseResult.average());
    // coefficients sum to 3/2, not exactly 1, so the course is not complete
    assertFalse(courseResult.complete());
    assertFalse(result.complete());
  }

  @Test
  void courseCreditsComeFromTheMostRecentAssignment() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var oldGroup = saveGroup(promotion);
    var newGroup = saveGroup(promotion);
    var student = saveStudent(promotion, oldGroup);
    var course = saveCourse(4);
    // The same course was assigned in two groups the student passed through, with different
    // credit values: the most recent assignment (highest academic year) must win.
    saveAssignment(course, oldGroup, 2023, 4);
    saveAssignment(course, newGroup, 2024, 6);

    groupFlowRepository.save(
        JGroupFlow.builder()
            .student(student)
            .group(newGroup)
            .groupFlowType(GroupFlowType.JOIN)
            .build());

    var result = getResult(token(admin), student.getId(), "L1");

    assertEquals(1, result.courses().size());
    assertEquals(6, result.courses().get(0).credits());
  }
}
