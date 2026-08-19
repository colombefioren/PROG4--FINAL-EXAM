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
import org.cocojojo.mg.endpoint.rest.controller.dto.ResultsSummaryResponse;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class ResultsSummaryIT extends FacadeIT {

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
    return "rs-" + prefix + SEQUENCE.incrementAndGet() + "@hei.school";
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
            .ref("RS-PROMO" + SEQUENCE.incrementAndGet())
            .name("RS Promotion " + SEQUENCE.incrementAndGet())
            .entryYear(2023)
            .build());
  }

  private JGroup saveGroup(JPromotion promotion) {
    return groupRepository.save(
        JGroup.builder()
            .promotion(promotion)
            .ref("RS-GRP" + SEQUENCE.incrementAndGet())
            .track(Track.TN)
            .build());
  }

  private JGroup saveGroup(JPromotion promotion, Track track) {
    return groupRepository.save(
        JGroup.builder()
            .promotion(promotion)
            .ref("RS-GRP" + SEQUENCE.incrementAndGet())
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
                .std("RS-STD" + SEQUENCE.incrementAndGet())
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

  private JCourse saveCourse(int credits, StudentLevel level) {
    return saveCourse(credits, level, level == StudentLevel.L1 ? null : Track.TN);
  }

  private JCourse saveCourse(int credits, StudentLevel level, Track track) {
    return courseRepository.save(
        JCourse.builder()
            .code("RS-CODE" + SEQUENCE.incrementAndGet())
            .name("Course " + SEQUENCE.incrementAndGet())
            .credits(credits)
            .totalHours(30)
            .studentLevel(level)
            .track(track)
            .build());
  }

  private JCourseAssignment saveAssignment(
      JCourse course, JGroup group, int credits, Semester semester) {
    return saveAssignment(course, group, credits, semester, 2023);
  }

  private JCourseAssignment saveAssignment(
      JCourse course, JGroup group, int credits, Semester semester, int academicYear) {
    return courseAssignmentRepository.save(
        JCourseAssignment.builder()
            .course(course)
            .group(group)
            .teachers(List.of(saveTeacher()))
            .academicYear(academicYear)
            .semester(semester)
            .credits(credits)
            .build());
  }

  private JExam saveExam(JCourseAssignment assignment) {
    return examRepository.save(
        JExam.builder()
            .courseAssignment(assignment)
            .title("Exam " + SEQUENCE.incrementAndGet())
            .examDatetime(Instant.parse("2024-06-01T08:00:00Z"))
            .coefficientNumerator(1)
            .coefficientDenominator(1)
            .build());
  }

  private JGrade saveGrade(JExam exam, JStudent student, BigDecimal value) {
    return gradeRepository.save(
        JGrade.builder().exam(exam).student(student).value(value).comment("ok").build());
  }

  private void gradeCompleteCourse(
      JStudent student,
      JGroup group,
      int credits,
      StudentLevel level,
      Semester semester,
      BigDecimal value) {
    var course = saveCourse(credits, level);
    var exam = saveExam(saveAssignment(course, group, credits, semester));
    saveGrade(exam, student, value);
  }

  private void gradeCompleteCourse(
      JStudent student,
      JGroup group,
      int credits,
      StudentLevel level,
      Semester semester,
      BigDecimal value,
      Track track) {
    var course = saveCourse(credits, level, track);
    var exam = saveExam(saveAssignment(course, group, credits, semester));
    saveGrade(exam, student, value);
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

  private ResultsSummaryResponse getSummary(String token, UUID studentId) {
    return webTestClient
        .get()
        .uri("/students/{id}/results-summary", studentId)
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(ResultsSummaryResponse.class)
        .returnResult()
        .getResponseBody();
  }

  @Test
  void adminCanReadResultsSummary() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    var summary = getSummary(token(admin), student.getId());

    assertEquals(student.getId(), summary.studentId());
    assertEquals(student.getStd(), summary.studentStd());
  }

  @Test
  void studentCanReadOwnResultsSummary() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    var summary = getSummary(token(student), student.getId());

    assertEquals(student.getId(), summary.studentId());
  }

  @Test
  void studentCannotReadAnotherStudentsSummary() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var other = saveStudent(promotion, group);
    var requester = saveStudent(promotion, group);

    webTestClient
        .get()
        .uri("/students/{id}/results-summary", other.getId())
        .header("Authorization", "Bearer " + token(requester))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void teacherCannotReadResultsSummary() {
    var teacher = saveTeacher();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    webTestClient
        .get()
        .uri("/students/{id}/results-summary", student.getId())
        .header("Authorization", "Bearer " + token(teacher))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void anonymousCannotReadResultsSummary() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    webTestClient
        .get()
        .uri("/students/{id}/results-summary", student.getId())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void unknownStudentIsNotFound() {
    var admin = saveAdmin();

    webTestClient
        .get()
        .uri("/students/{id}/results-summary", UUID.randomUUID())
        .header("Authorization", "Bearer " + token(admin))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void emptyStudentReturnsEmptySummary() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    var summary = getSummary(token(admin), student.getId());

    assertEquals(3, summary.levels().size());
    assertEquals(
        List.of(StudentLevel.L1, StudentLevel.L2, StudentLevel.L3),
        summary.levels().stream().map(l -> l.level()).toList());
    assertTrue(summary.levels().stream().allMatch(l -> l.courses().isEmpty()));
    assertNull(summary.overallAverage());
    assertFalse(summary.graduate());
  }

  @Test
  void graduateWhenAllLevelsComplete() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    gradeCompleteCourse(student, group, 4, StudentLevel.L1, Semester.S1, new BigDecimal("14"));
    gradeCompleteCourse(student, group, 4, StudentLevel.L2, Semester.S3, new BigDecimal("12"));
    gradeCompleteCourse(student, group, 4, StudentLevel.L3, Semester.S5, new BigDecimal("13"));

    var summary = getSummary(token(admin), student.getId());

    assertTrue(summary.levels().stream().allMatch(l -> l.status() == ResultStatus.COMPLETED));
    assertEquals(new BigDecimal("13.00"), summary.overallAverage());
    assertEquals(12, summary.levels().stream().mapToInt(l -> l.earnedCredits()).sum());
    assertTrue(summary.graduate());
  }

  @Test
  void nonGraduateWhenOneLevelFailing() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    gradeCompleteCourse(student, group, 4, StudentLevel.L1, Semester.S1, new BigDecimal("14"));
    gradeCompleteCourse(student, group, 4, StudentLevel.L2, Semester.S3, new BigDecimal("8"));

    var summary = getSummary(token(admin), student.getId());

    assertEquals(ResultStatus.COMPLETED, summary.levels().get(0).status());
    assertEquals(ResultStatus.PROVISIONAL, summary.levels().get(1).status());
    assertEquals(ResultStatus.PROVISIONAL, summary.levels().get(2).status());
    assertEquals(new BigDecimal("11.00"), summary.overallAverage());
    assertFalse(summary.graduate());
  }

  @Test
  void overallAverageIsCreditWeightedAcrossLevels() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    gradeCompleteCourse(student, group, 4, StudentLevel.L1, Semester.S1, new BigDecimal("14"));
    gradeCompleteCourse(student, group, 2, StudentLevel.L2, Semester.S3, new BigDecimal("10"));

    var summary = getSummary(token(admin), student.getId());

    assertEquals(new BigDecimal("12.67"), summary.overallAverage());
    assertEquals(6, summary.levels().stream().mapToInt(l -> l.totalCredits()).sum());
  }

  @Test
  void retakeCountsOnlyTheLatestAttempt() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);
    var course = saveCourse(4, StudentLevel.L1);

    var firstAttempt = saveAssignment(course, group, 4, Semester.S1, 2023);
    var retake = saveAssignment(course, group, 6, Semester.S1, 2024);
    saveGrade(saveExam(firstAttempt), student, new BigDecimal("8"));
    saveGrade(saveExam(retake), student, new BigDecimal("14"));

    var summary = getSummary(token(admin), student.getId());
    var l1 =
        summary.levels().stream()
            .filter(l -> l.level() == StudentLevel.L1)
            .findFirst()
            .orElseThrow();
    var courseResult = l1.courses().get(0);

    
    
    assertEquals(new BigDecimal("14.00"), courseResult.average());
    assertEquals(6, courseResult.credits());
    assertTrue(courseResult.complete());
    assertTrue(courseResult.passed());
    assertEquals(ResultStatus.COMPLETED, l1.status());
  }

  @Test
  void trackSwitchKeepsOnlyCurrentTrackCoursesRequired() {
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

    
    
    gradeCompleteCourse(
        student, tnGroup, 4, StudentLevel.L1, Semester.S1, new BigDecimal("14"), null);
    gradeCompleteCourse(
        student, elGroup, 4, StudentLevel.L2, Semester.S3, new BigDecimal("12"), Track.EL);
    gradeCompleteCourse(
        student, elGroup, 4, StudentLevel.L3, Semester.S5, new BigDecimal("13"), Track.EL);
    gradeCompleteCourse(
        student, tnGroup, 4, StudentLevel.L2, Semester.S3, new BigDecimal("8"), Track.TN);
    gradeCompleteCourse(
        student, tnGroup, 4, StudentLevel.L3, Semester.S5, new BigDecimal("9"), Track.TN);

    var summary = getSummary(token(admin), student.getId());

    
    var l2 =
        summary.levels().stream()
            .filter(l -> l.level() == StudentLevel.L2)
            .findFirst()
            .orElseThrow();
    assertEquals(1, l2.courses().size());
    assertEquals(Track.EL, l2.courses().get(0).track());
    assertTrue(summary.levels().stream().allMatch(l -> l.status() == ResultStatus.COMPLETED));
    assertEquals(new BigDecimal("13.00"), summary.overallAverage());
    assertTrue(summary.graduate());
  }
}
