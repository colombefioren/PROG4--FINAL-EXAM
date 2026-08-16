package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.GraduateExportResponse;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.file.bucket.BucketComponent;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GraduatesExportIT extends FacadeIT {

  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  @MockBean private BucketComponent bucketComponent;

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
  @SneakyThrows
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
    when(bucketComponent.presign(any(), any()))
        .thenReturn(
            URI.create("https://dummy-bucket.s3.eu-west-3.amazonaws.com/graduates/list.xlsx")
                .toURL());
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

  private JGroup saveGroup(JPromotion promotion) {
    return groupRepository.save(
        JGroup.builder()
            .promotion(promotion)
            .ref("GRP" + SEQUENCE.incrementAndGet())
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

  private void giveFullCurriculum(JGroup group, JStudent student) {
    var levels = List.of(StudentLevel.L1, StudentLevel.L2, StudentLevel.L3);
    var semesters = List.of(Semester.S1, Semester.S3, Semester.S5);
    var teacher = saveTeacher();
    for (int i = 0; i < levels.size(); i++) {
      var course =
          courseRepository.save(
              JCourse.builder()
                  .code("CODE" + SEQUENCE.incrementAndGet())
                  .name("Course " + SEQUENCE.incrementAndGet())
                  .credits(4)
                  .totalHours(30)
                  .studentLevel(levels.get(i))
                  .track(Track.TN)
                  .build());
      var assignment =
          courseAssignmentRepository.save(
              JCourseAssignment.builder()
                  .course(course)
                  .group(group)
                  .teachers(List.of(teacher))
                  .academicYear(2024)
                  .semester(semesters.get(i))
                  .credits(course.getCredits())
                  .build());
      var exam =
          examRepository.save(
              JExam.builder()
                  .courseAssignment(assignment)
                  .title("Exam " + SEQUENCE.incrementAndGet())
                  .examDatetime(Instant.parse("2025-06-01T08:00:00Z"))
                  .coefficientNumerator(1)
                  .coefficientDenominator(1)
                  .build());
      gradeRepository.save(
          JGrade.builder().exam(exam).student(student).value(new BigDecimal("14")).build());
    }
  }

  private String token(JAdmin admin) {
    return jwtService.generateToken(admin.getId(), admin.getEmail(), Role.ADMIN);
  }

  private String token(JTeacher teacher) {
    return jwtService.generateToken(teacher.getId(), teacher.getEmail(), Role.TEACHER);
  }

  @Test
  void adminCanExportGraduateListAsPresignedUrl() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);
    giveFullCurriculum(group, student);

    var body =
        webTestClient
            .get()
            .uri("/promotions/{promotion_id}/graduates/export", promotion.getId())
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
        .uri("/promotions/{promotion_id}/graduates/export", promotion.getId())
        .header("Authorization", "Bearer " + token(teacher))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void unauthenticatedCannotExportGraduateList() {
    webTestClient
        .get()
        .uri("/promotions/{promotion_id}/graduates/export", UUID.randomUUID())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }
}
