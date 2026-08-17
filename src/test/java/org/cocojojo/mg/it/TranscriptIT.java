package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.event.EventProducer;
import org.cocojojo.mg.endpoint.event.model.TranscriptRequested;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.file.bucket.BucketComponent;
import org.cocojojo.mg.mail.Email;
import org.cocojojo.mg.mail.Mailer;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
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
import org.cocojojo.mg.service.event.TranscriptRequestedService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

class TranscriptIT extends FacadeIT {

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
  @Autowired private TranscriptRequestedService transcriptRequestedService;

  @MockBean private EventProducer<TranscriptRequested> eventProducer;
  @MockBean private BucketComponent bucketComponent;
  @MockBean private Mailer mailer;

  @LocalServerPort int port;
  private WebTestClient webTestClient;

  @BeforeEach
  @SneakyThrows
  void setUp() {
    webTestClient =
        WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .responseTimeout(Duration.ofSeconds(30))
            .build();
    when(bucketComponent.presign(any(), any()))
        .thenReturn(
            URI.create("https://dummy-bucket.s3.eu-west-3.amazonaws.com/transcripts/test.pdf")
                .toURL());
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
    return "tr-" + prefix + SEQUENCE.incrementAndGet() + "@hei.school";
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
            .ref("TR-PROMO" + SEQUENCE.incrementAndGet())
            .name("TR Promotion " + SEQUENCE.incrementAndGet())
            .entryYear(2023)
            .build());
  }

  private JGroup saveGroup(JPromotion promotion) {
    return groupRepository.save(
        JGroup.builder()
            .promotion(promotion)
            .ref("TR-GRP" + SEQUENCE.incrementAndGet())
            .track(null)
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
                .std("TR-STD" + SEQUENCE.incrementAndGet())
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

  private JCourse saveCourse() {
    return courseRepository.save(
        JCourse.builder()
            .code("TR-CODE" + SEQUENCE.incrementAndGet())
            .name("Course " + SEQUENCE.incrementAndGet())
            .credits(4)
            .totalHours(30)
            .studentLevel(StudentLevel.L1)
            .track(null)
            .build());
  }

  private JCourseAssignment saveAssignment(JCourse course, JGroup group) {
    return courseAssignmentRepository.save(
        JCourseAssignment.builder()
            .course(course)
            .group(group)
            .teachers(List.of(saveTeacher()))
            .academicYear(2024)
            .semester(Semester.S1)
            .credits(course.getCredits())
            .build());
  }

  private JExam saveExam(JCourseAssignment assignment, int numerator, int denominator) {
    return examRepository.save(
        JExam.builder()
            .courseAssignment(assignment)
            .title("Final " + SEQUENCE.incrementAndGet())
            .examDatetime(Instant.parse("2025-06-01T08:00:00Z"))
            .coefficientNumerator(numerator)
            .coefficientDenominator(denominator)
            .build());
  }

  private void saveGrade(JExam exam, JStudent student, BigDecimal value) {
    gradeRepository.save(JGrade.builder().exam(exam).student(student).value(value).build());
  }

  @SneakyThrows
  private String pdfText(File pdf) {
    try (var document = PDDocument.load(pdf)) {
      return new PDFTextStripper().getText(document);
    }
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

  @Test
  void adminCanRequestTranscriptAndEventIsEnqueued() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    webTestClient
        .post()
        .uri("/students/{id}/yearly_results/{level}/transcript", student.getId(), "L1")
        .header("Authorization", "Bearer " + token(admin))
        .exchange()
        .expectStatus()
        .isAccepted();

    var captor = ArgumentCaptor.forClass(Collection.class);
    verify(eventProducer).accept(captor.capture());
    var event = (TranscriptRequested) captor.getValue().iterator().next();
    assertEquals(student.getId().toString(), event.getStudentId());
    assertEquals("L1", event.getLevel());
  }

  @Test
  void studentCanRequestOwnTranscript() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    webTestClient
        .post()
        .uri("/students/{id}/yearly_results/{level}/transcript", student.getId(), "L2")
        .header("Authorization", "Bearer " + token(student))
        .exchange()
        .expectStatus()
        .isAccepted();
  }

  @Test
  void teacherCannotRequestTranscript() {
    var teacher = saveTeacher();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    webTestClient
        .post()
        .uri("/students/{id}/yearly_results/{level}/transcript", student.getId(), "L1")
        .header("Authorization", "Bearer " + token(teacher))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void studentCannotRequestAnotherStudentsTranscript() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var other = saveStudent(promotion, group);
    var requester = saveStudent(promotion, group);

    webTestClient
        .post()
        .uri("/students/{id}/yearly_results/{level}/transcript", other.getId(), "L1")
        .header("Authorization", "Bearer " + token(requester))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void anonymousCannotRequestTranscript() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    webTestClient
        .post()
        .uri("/students/{id}/yearly_results/{level}/transcript", student.getId(), "L1")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void unknownStudentIsNotFound() {
    var admin = saveAdmin();

    webTestClient
        .post()
        .uri("/students/{id}/yearly_results/{level}/transcript", UUID.randomUUID(), "L1")
        .header("Authorization", "Bearer " + token(admin))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  @SneakyThrows
  void handlerRendersPdfUploadsToS3AndEmailsTheLink() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);
    var course = saveCourse();
    var assignment = saveAssignment(course, group);
    var exam = saveExam(assignment, 1, 1);
    saveGrade(exam, student, new BigDecimal("14"));

    transcriptRequestedService.accept(
        TranscriptRequested.builder().studentId(student.getId().toString()).level("L1").build());

    var uploadCaptor = ArgumentCaptor.forClass(File.class);
    var keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(bucketComponent).upload(uploadCaptor.capture(), keyCaptor.capture());
    assertTrue(uploadCaptor.getValue().getName().startsWith("transcript-" + student.getStd()));
    assertTrue(keyCaptor.getValue().startsWith("transcripts/" + student.getStd() + "/"));
    assertTrue(keyCaptor.getValue().endsWith(".pdf"));
    assertTrue(uploadCaptor.getValue().length() > 100); // a real PDF was produced

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());
    assertEquals(student.getEmail(), emailCaptor.getValue().to().getAddress());
    assertTrue(emailCaptor.getValue().htmlBody().contains("dummy-bucket"));
  }

  @Test
  @SneakyThrows
  void transcriptLabelsUngradedCourseAsNotGradedYet() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);
    var course = saveCourse();
    saveAssignment(course, group); // no exam, no grade

    transcriptRequestedService.accept(
        TranscriptRequested.builder().studentId(student.getId().toString()).level("L1").build());

    var uploadCaptor = ArgumentCaptor.forClass(File.class);
    verify(bucketComponent).upload(uploadCaptor.capture(), any());
    var text = pdfText(uploadCaptor.getValue());
    assertTrue(text.contains("Not graded yet"));
    assertFalse(text.contains("Passed"));
  }

  @Test
  @SneakyThrows
  void transcriptLabelsPassedCourseAsPassed() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);
    var course = saveCourse();
    var assignment = saveAssignment(course, group);
    saveGrade(saveExam(assignment, 1, 1), student, new BigDecimal("14"));

    transcriptRequestedService.accept(
        TranscriptRequested.builder().studentId(student.getId().toString()).level("L1").build());

    var uploadCaptor = ArgumentCaptor.forClass(File.class);
    verify(bucketComponent).upload(uploadCaptor.capture(), any());
    var text = pdfText(uploadCaptor.getValue());
    assertTrue(text.contains("Passed"));
    assertFalse(text.contains("Not passed"));
  }

  @Test
  @SneakyThrows
  void transcriptLabelsFailedCourseAsNotPassed() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);
    var course = saveCourse();
    var assignment = saveAssignment(course, group);
    saveGrade(saveExam(assignment, 1, 1), student, new BigDecimal("8"));

    transcriptRequestedService.accept(
        TranscriptRequested.builder().studentId(student.getId().toString()).level("L1").build());

    var uploadCaptor = ArgumentCaptor.forClass(File.class);
    verify(bucketComponent).upload(uploadCaptor.capture(), any());
    var text = pdfText(uploadCaptor.getValue());
    assertTrue(text.contains("Not passed"));
  }

  @Test
  @SneakyThrows
  void handlerSkipsEmptyCourseList() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group);

    transcriptRequestedService.accept(
        TranscriptRequested.builder().studentId(student.getId().toString()).level("L3").build());

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());
    assertEquals(student.getEmail(), emailCaptor.getValue().to().getAddress());
  }
}
