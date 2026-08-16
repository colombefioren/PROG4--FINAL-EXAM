package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.GraduateResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
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
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtService jwtService;

  @LocalServerPort int port;
  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    gradeRepository.deleteAll();
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
    return prefix + SEQUENCE.incrementAndGet() + "@hei.school";
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
            .ref("PROMO" + SEQUENCE.incrementAndGet())
            .name("Promotion " + SEQUENCE.incrementAndGet())
            .entryYear(2023)
            .build());
  }

  private JGroup saveGroup(JPromotion promotion, Track track) {
    return groupRepository.save(
        JGroup.builder()
            .promotion(promotion)
            .ref("GRP" + SEQUENCE.incrementAndGet())
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
                .std("STD" + SEQUENCE.incrementAndGet())
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
            .code("CODE" + SEQUENCE.incrementAndGet())
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
            .uri("/promotions/{promotion_id}/graduates", promotionId)
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

  /** Curriculum: one course per level assigned to the student's group. */
  private void giveFullCurriculum(JGroup group, JStudent student, BigDecimal[] grades) {
    var courses =
        List.of(
            saveCourse(StudentLevel.L1, null),
            saveCourse(StudentLevel.L2, group.getTrack()),
            saveCourse(StudentLevel.L3, group.getTrack()));
    var semesters = List.of(Semester.S1, Semester.S3, Semester.S5);
    for (int i = 0; i < courses.size(); i++) {
      var exam = saveExam(saveAssignment(courses.get(i), group, semesters.get(i)));
      saveGrade(exam, student, grades[i]);
    }
  }

  @Test
  void adminCanListGraduatesRankedByDescendingAverage() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var topGroup = saveGroup(promotion, Track.TN);
    var lowerGroup = saveGroup(promotion, Track.TN);
    var topStudent = saveStudent(promotion, topGroup);
    var lowerStudent = saveStudent(promotion, lowerGroup);
    giveFullCurriculum(
        topGroup,
        topStudent,
        new BigDecimal[] {new BigDecimal("16"), new BigDecimal("15"), new BigDecimal("17")});
    giveFullCurriculum(
        lowerGroup,
        lowerStudent,
        new BigDecimal[] {new BigDecimal("11"), new BigDecimal("12"), new BigDecimal("13")});

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
    var passingGroup = saveGroup(promotion, Track.TN);
    var failingGroup = saveGroup(promotion, Track.TN);
    var passingStudent = saveStudent(promotion, passingGroup);
    var failingStudent = saveStudent(promotion, failingGroup);
    giveFullCurriculum(
        passingGroup,
        passingStudent,
        new BigDecimal[] {new BigDecimal("14"), new BigDecimal("15"), new BigDecimal("16")});
    giveFullCurriculum(
        failingGroup,
        failingStudent,
        new BigDecimal[] {new BigDecimal("12"), new BigDecimal("8"), new BigDecimal("13")});

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
    var otherStudent = saveStudent(otherPromotion, otherGroup);
    giveFullCurriculum(
        group,
        student,
        new BigDecimal[] {new BigDecimal("14"), new BigDecimal("15"), new BigDecimal("16")});
    giveFullCurriculum(
        otherGroup,
        otherStudent,
        new BigDecimal[] {new BigDecimal("14"), new BigDecimal("15"), new BigDecimal("16")});

    var graduates = getGraduates(token(admin), promotion.getId());

    assertEquals(1, graduates.size());
    assertEquals(student.getStd(), graduates.get(0).std());
  }

  @Test
  void teacherCannotAccessGraduates() {
    var teacher = saveTeacher();
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var student = saveStudent(promotion, group);
    giveFullCurriculum(
        group,
        student,
        new BigDecimal[] {new BigDecimal("14"), new BigDecimal("15"), new BigDecimal("16")});

    webTestClient
        .get()
        .uri("/promotions/{promotion_id}/graduates", promotion.getId())
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
        .uri("/promotions/{promotion_id}/graduates", student.getPromotion().getId())
        .header("Authorization", "Bearer " + token(student))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void unauthenticatedCannotAccessGraduates() {
    webTestClient
        .get()
        .uri("/promotions/{promotion_id}/graduates", UUID.randomUUID())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void unknownPromotionReturnsNotFound() {
    var admin = saveAdmin();

    webTestClient
        .get()
        .uri("/promotions/{promotion_id}/graduates", UUID.randomUUID())
        .header("Authorization", "Bearer " + token(admin))
        .exchange()
        .expectStatus()
        .isNotFound();
  }
}
