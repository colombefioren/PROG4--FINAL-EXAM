package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeCorrectionRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeHistoryResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GradeResponse;
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

class GradeIT extends FacadeIT {

  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  @Autowired private AdminRepository adminRepository;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private CourseAssignmentRepository courseAssignmentRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private GradeRepository gradeRepository;
  @Autowired private GroupFlowRepository groupFlowRepository;
  @Autowired private GradeHistoryRepository gradeHistoryRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtService jwtService;

  @LocalServerPort int port;
  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    gradeHistoryRepository.deleteAll();
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

  private JStudent saveStudent() {
    return studentRepository.save(
        JStudent.builder()
            .firstname("Alan")
            .lastname("Turing")
            .email(unique("student"))
            .password(passwordEncoder.encode("secret123"))
            .std("STD" + SEQUENCE.incrementAndGet())
            .promotion(savePromotion())
            .build());
  }

  private JPromotion savePromotion() {
    return promotionRepository.save(
        JPromotion.builder()
            .ref("PROMO" + SEQUENCE.incrementAndGet())
            .name("Promotion " + SEQUENCE.incrementAndGet())
            .entryYear(2025)
            .build());
  }

  private JCourse saveCourse() {
    return courseRepository.save(
        JCourse.builder()
            .code("CODE" + SEQUENCE.incrementAndGet())
            .name("Course " + SEQUENCE.incrementAndGet())
            .credits(4)
            .totalHours(30)
            .studentLevel(StudentLevel.L3)
            .track(Track.TN)
            .build());
  }

  private JGroup saveGroup() {
    return groupRepository.save(
        JGroup.builder()
            .promotion(savePromotion())
            .ref("REF" + SEQUENCE.incrementAndGet())
            .track(Track.TN)
            .build());
  }

  private JCourseAssignment saveAssignment(JTeacher teacher) {
    return courseAssignmentRepository.save(
        JCourseAssignment.builder()
            .course(saveCourse())
            .group(saveGroup())
            .teachers(List.of(teacher))
            .academicYear(2025)
            .semester(Semester.S1)
            .credits(4)
            .build());
  }

  private JExam saveExam(JCourseAssignment assignment) {
    return examRepository.save(
        JExam.builder()
            .courseAssignment(assignment)
            .title("Exam " + SEQUENCE.incrementAndGet())
            .examDatetime(Instant.parse("2026-06-01T08:00:00Z"))
            .coefficientNumerator(1)
            .coefficientDenominator(1)
            .build());
  }

  private JGrade saveGrade(JExam exam, JStudent student, BigDecimal value) {
    return gradeRepository.save(
        JGrade.builder().exam(exam).student(student).value(value).comment("ok").build());
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

  private List<GradeResponse> upsertGrades(String token, UUID examId, List<GradeRequest> requests) {
    var body =
        webTestClient
            .put()
            .uri("/exams/{exam_id}/grades", examId)
            .header("Authorization", "Bearer " + token)
            .bodyValue(requests)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(GradeResponse.class)
            .returnResult()
            .getResponseBody();
    assertNotNull(body);
    return body;
  }

  @Test
  void adminCanListAndUpsertGradesForExam() {
    var admin = saveAdmin();
    var exam = saveExam(saveAssignment(saveTeacher()));
    var student = saveStudent();

    var created =
        upsertGrades(
            token(admin),
            exam.getId(),
            List.of(
                GradeRequest.builder()
                    .studentId(student.getId())
                    .examId(exam.getId())
                    .value(new BigDecimal("15.5"))
                    .comment("good")
                    .build()));

    assertEquals(1, created.size());
    assertEquals(student.getId(), created.get(0).studentId());

    var listed =
        webTestClient
            .get()
            .uri("/exams/{exam_id}/grades", exam.getId())
            .header("Authorization", "Bearer " + token(admin))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(GradeResponse.class)
            .returnResult()
            .getResponseBody();
    assertNotNull(listed);
    assertEquals(1, listed.size());
    assertEquals(0, new BigDecimal("15.5").compareTo(listed.get(0).value()));
  }

  @Test
  void teacherCanUpsertGradesForCourseTheyTeach() {
    var teacher = saveTeacher();
    var assignment = saveAssignment(teacher);
    var exam = saveExam(assignment);
    var student = saveStudent();

    var created =
        upsertGrades(
            token(teacher),
            exam.getId(),
            List.of(
                GradeRequest.builder()
                    .studentId(student.getId())
                    .examId(exam.getId())
                    .value(new BigDecimal("12.0"))
                    .build()));

    assertEquals(1, created.size());
    assertEquals(student.getId(), created.get(0).studentId());
  }

  @Test
  void teacherCannotManageGradesForCourseTheyDoNotTeach() {
    var teacher = saveTeacher();
    var otherTeacher = saveTeacher();
    var exam = saveExam(saveAssignment(otherTeacher));
    var student = saveStudent();
    var request =
        List.of(
            GradeRequest.builder()
                .studentId(student.getId())
                .examId(exam.getId())
                .value(new BigDecimal("10.0"))
                .build());

    webTestClient
        .put()
        .uri("/exams/{exam_id}/grades", exam.getId())
        .header("Authorization", "Bearer " + token(teacher))
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isForbidden();

    webTestClient
        .get()
        .uri("/exams/{exam_id}/grades", exam.getId())
        .header("Authorization", "Bearer " + token(teacher))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void studentCannotAccessExamGrades() {
    var student = saveStudent();
    var exam = saveExam(saveAssignment(saveTeacher()));
    var request =
        List.of(
            GradeRequest.builder()
                .studentId(student.getId())
                .examId(exam.getId())
                .value(new BigDecimal("10.0"))
                .build());

    webTestClient
        .get()
        .uri("/exams/{exam_id}/grades", exam.getId())
        .header("Authorization", "Bearer " + token(student))
        .exchange()
        .expectStatus()
        .isForbidden();

    webTestClient
        .put()
        .uri("/exams/{exam_id}/grades", exam.getId())
        .header("Authorization", "Bearer " + token(student))
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void unauthenticatedCannotAccessExamGrades() {
    var exam = saveExam(saveAssignment(saveTeacher()));

    webTestClient
        .get()
        .uri("/exams/{exam_id}/grades", exam.getId())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void unknownExamReturnsNotFound() {
    var admin = saveAdmin();

    webTestClient
        .get()
        .uri("/exams/{exam_id}/grades", UUID.randomUUID())
        .header("Authorization", "Bearer " + token(admin))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void examIdMismatchIsRejected() {
    var admin = saveAdmin();
    var exam = saveExam(saveAssignment(saveTeacher()));
    var student = saveStudent();
    var request =
        List.of(
            GradeRequest.builder()
                .studentId(student.getId())
                .examId(UUID.randomUUID())
                .value(new BigDecimal("10.0"))
                .build());

    webTestClient
        .put()
        .uri("/exams/{exam_id}/grades", exam.getId())
        .header("Authorization", "Bearer " + token(admin))
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void historyRecordsNullPreviousOnFirstGrade() {
    var admin = saveAdmin();
    var exam = saveExam(saveAssignment(saveTeacher()));
    var student = saveStudent();

    upsertGrades(
        token(admin),
        exam.getId(),
        List.of(
            GradeRequest.builder()
                .studentId(student.getId())
                .examId(exam.getId())
                .value(new BigDecimal("14.0"))
                .build()));

    var grade = gradeRepository.findAll().get(0);
    var history =
        webTestClient
            .get()
            .uri("/grades/{grade_id}/history", grade.getId())
            .header("Authorization", "Bearer " + token(admin))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(GradeHistoryResponse.class)
            .returnResult()
            .getResponseBody();
    assertNotNull(history);
    assertEquals(1, history.size());
    assertNull(history.get(0).previousValue());
    assertEquals(0, new BigDecimal("14.0").compareTo(history.get(0).newValue()));
  }

  @Test
  void correctionUpdatesValueAndRecordsHistory() {
    var admin = saveAdmin();
    var exam = saveExam(saveAssignment(saveTeacher()));
    var student = saveStudent();

    upsertGrades(
        token(admin),
        exam.getId(),
        List.of(
            GradeRequest.builder()
                .studentId(student.getId())
                .examId(exam.getId())
                .value(new BigDecimal("14.0"))
                .build()));
    var grade = gradeRepository.findAll().get(0);

    var corrected =
        webTestClient
            .put()
            .uri("/exams/{exam_id}/students/{student_id}/grade", exam.getId(), student.getId())
            .header("Authorization", "Bearer " + token(admin))
            .bodyValue(
                GradeCorrectionRequest.builder()
                    .value(new BigDecimal("16.5"))
                    .reason("Recheck")
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(GradeResponse.class)
            .returnResult()
            .getResponseBody();
    assertNotNull(corrected);
    assertEquals(0, new BigDecimal("16.5").compareTo(corrected.value()));

    var history =
        webTestClient
            .get()
            .uri("/grades/{grade_id}/history", grade.getId())
            .header("Authorization", "Bearer " + token(admin))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(GradeHistoryResponse.class)
            .returnResult()
            .getResponseBody();
    assertNotNull(history);
    assertEquals(2, history.size());
    assertEquals(0, new BigDecimal("14.0").compareTo(history.get(0).previousValue()));
    assertEquals(0, new BigDecimal("16.5").compareTo(history.get(0).newValue()));
    assertEquals("Recheck", history.get(0).reason());
  }

  @Test
  void studentCanViewOwnGrades() {
    var student = saveStudent();
    var exam = saveExam(saveAssignment(saveTeacher()));
    saveGrade(exam, student, new BigDecimal("12.0"));

    var grades =
        webTestClient
            .get()
            .uri("/students/{student_id}/grades", student.getId())
            .header("Authorization", "Bearer " + token(student))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(GradeResponse.class)
            .returnResult()
            .getResponseBody();
    assertNotNull(grades);
    assertEquals(1, grades.size());
    assertEquals(student.getId(), grades.get(0).studentId());
  }

  @Test
  void studentCannotViewAnotherStudentsGrades() {
    var student = saveStudent();
    var other = saveStudent();
    var exam = saveExam(saveAssignment(saveTeacher()));
    saveGrade(exam, student, new BigDecimal("12.0"));

    webTestClient
        .get()
        .uri("/students/{student_id}/grades", student.getId())
        .header("Authorization", "Bearer " + token(other))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void teacherSeesOnlyGradesForCoursesTheyTeach() {
    var teacher = saveTeacher();
    var otherTeacher = saveTeacher();
    var student = saveStudent();
    var taughtExam = saveExam(saveAssignment(teacher));
    var otherExam = saveExam(saveAssignment(otherTeacher));
    saveGrade(taughtExam, student, new BigDecimal("12.0"));
    saveGrade(otherExam, student, new BigDecimal("8.0"));

    var grades =
        webTestClient
            .get()
            .uri("/students/{student_id}/grades", student.getId())
            .header("Authorization", "Bearer " + token(teacher))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(GradeResponse.class)
            .returnResult()
            .getResponseBody();
    assertNotNull(grades);
    assertEquals(1, grades.size());
    assertEquals(taughtExam.getId(), grades.get(0).examId());
  }

  @Test
  void adminCanViewAnyStudentsGrades() {
    var admin = saveAdmin();
    var student = saveStudent();
    var exam = saveExam(saveAssignment(saveTeacher()));
    saveGrade(exam, student, new BigDecimal("12.0"));

    var grades =
        webTestClient
            .get()
            .uri("/students/{student_id}/grades", student.getId())
            .header("Authorization", "Bearer " + token(admin))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(GradeResponse.class)
            .returnResult()
            .getResponseBody();
    assertNotNull(grades);
    assertEquals(1, grades.size());
  }

  @Test
  void studentCanGetOwnGradeById() {
    var student = saveStudent();
    var exam = saveExam(saveAssignment(saveTeacher()));
    var grade = saveGrade(exam, student, new BigDecimal("12.0"));

    webTestClient
        .get()
        .uri("/grades/{grade_id}", grade.getId())
        .header("Authorization", "Bearer " + token(student))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(GradeResponse.class)
        .returnResult()
        .getResponseBody();
  }

  @Test
  void studentCannotGetAnotherStudentsGradeById() {
    var student = saveStudent();
    var other = saveStudent();
    var exam = saveExam(saveAssignment(saveTeacher()));
    var grade = saveGrade(exam, student, new BigDecimal("12.0"));

    webTestClient
        .get()
        .uri("/grades/{grade_id}", grade.getId())
        .header("Authorization", "Bearer " + token(other))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void teacherCanGetGradeForCourseTheyTeach() {
    var teacher = saveTeacher();
    var student = saveStudent();
    var exam = saveExam(saveAssignment(teacher));
    var grade = saveGrade(exam, student, new BigDecimal("12.0"));

    webTestClient
        .get()
        .uri("/grades/{grade_id}", grade.getId())
        .header("Authorization", "Bearer " + token(teacher))
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void teacherCannotGetGradeForCourseTheyDoNotTeach() {
    var teacher = saveTeacher();
    var otherTeacher = saveTeacher();
    var student = saveStudent();
    var exam = saveExam(saveAssignment(otherTeacher));
    var grade = saveGrade(exam, student, new BigDecimal("12.0"));

    webTestClient
        .get()
        .uri("/grades/{grade_id}", grade.getId())
        .header("Authorization", "Bearer " + token(teacher))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void adminCanGetAnyGradeById() {
    var admin = saveAdmin();
    var student = saveStudent();
    var exam = saveExam(saveAssignment(saveTeacher()));
    var grade = saveGrade(exam, student, new BigDecimal("12.0"));

    webTestClient
        .get()
        .uri("/grades/{grade_id}", grade.getId())
        .header("Authorization", "Bearer " + token(admin))
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void adminCanDeleteGrade() {
    var admin = saveAdmin();
    var student = saveStudent();
    var exam = saveExam(saveAssignment(saveTeacher()));
    var grade = saveGrade(exam, student, new BigDecimal("12.0"));

    webTestClient
        .delete()
        .uri("/grades/{grade_id}", grade.getId())
        .header("Authorization", "Bearer " + token(admin))
        .exchange()
        .expectStatus()
        .isNoContent();

    webTestClient
        .get()
        .uri("/grades/{grade_id}", grade.getId())
        .header("Authorization", "Bearer " + token(admin))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void teacherCanDeleteGradeForCourseTheyTeach() {
    var teacher = saveTeacher();
    var student = saveStudent();
    var exam = saveExam(saveAssignment(teacher));
    var grade = saveGrade(exam, student, new BigDecimal("12.0"));

    webTestClient
        .delete()
        .uri("/grades/{grade_id}", grade.getId())
        .header("Authorization", "Bearer " + token(teacher))
        .exchange()
        .expectStatus()
        .isNoContent();
  }

  @Test
  void teacherCannotDeleteGradeForCourseTheyDoNotTeach() {
    var teacher = saveTeacher();
    var otherTeacher = saveTeacher();
    var student = saveStudent();
    var exam = saveExam(saveAssignment(otherTeacher));
    var grade = saveGrade(exam, student, new BigDecimal("12.0"));

    webTestClient
        .delete()
        .uri("/grades/{grade_id}", grade.getId())
        .header("Authorization", "Bearer " + token(teacher))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void studentCannotDeleteGrade() {
    var student = saveStudent();
    var exam = saveExam(saveAssignment(saveTeacher()));
    var grade = saveGrade(exam, student, new BigDecimal("12.0"));

    webTestClient
        .delete()
        .uri("/grades/{grade_id}", grade.getId())
        .header("Authorization", "Bearer " + token(student))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void teacherCanViewHistoryForCourseTheyTeach() {
    var teacher = saveTeacher();
    var student = saveStudent();
    var exam = saveExam(saveAssignment(teacher));
    var grade = saveGrade(exam, student, new BigDecimal("12.0"));

    webTestClient
        .get()
        .uri("/grades/{grade_id}/history", grade.getId())
        .header("Authorization", "Bearer " + token(teacher))
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void teacherCannotViewHistoryForCourseTheyDoNotTeach() {
    var teacher = saveTeacher();
    var otherTeacher = saveTeacher();
    var student = saveStudent();
    var exam = saveExam(saveAssignment(otherTeacher));
    var grade = saveGrade(exam, student, new BigDecimal("12.0"));

    webTestClient
        .get()
        .uri("/grades/{grade_id}/history", grade.getId())
        .header("Authorization", "Bearer " + token(teacher))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void studentCannotViewHistory() {
    var student = saveStudent();
    var exam = saveExam(saveAssignment(saveTeacher()));
    var grade = saveGrade(exam, student, new BigDecimal("12.0"));

    webTestClient
        .get()
        .uri("/grades/{grade_id}/history", grade.getId())
        .header("Authorization", "Bearer " + token(student))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void upsertUpdateRecordsHistoryWithPreviousValue() {
    var admin = saveAdmin();
    var exam = saveExam(saveAssignment(saveTeacher()));
    var student = saveStudent();

    upsertGrades(
        token(admin),
        exam.getId(),
        List.of(
            GradeRequest.builder()
                .studentId(student.getId())
                .examId(exam.getId())
                .value(new BigDecimal("14.0"))
                .build()));
    upsertGrades(
        token(admin),
        exam.getId(),
        List.of(
            GradeRequest.builder()
                .studentId(student.getId())
                .examId(exam.getId())
                .value(new BigDecimal("17.0"))
                .build()));

    var grade = gradeRepository.findAll().get(0);
    assertEquals(0, new BigDecimal("17.0").compareTo(grade.getValue()));
    var history =
        webTestClient
            .get()
            .uri("/grades/{grade_id}/history", grade.getId())
            .header("Authorization", "Bearer " + token(admin))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(GradeHistoryResponse.class)
            .returnResult()
            .getResponseBody();
    assertNotNull(history);
    assertEquals(2, history.size());
    assertEquals(0, new BigDecimal("14.0").compareTo(history.get(0).previousValue()));
    assertEquals(0, new BigDecimal("17.0").compareTo(history.get(0).newValue()));
  }

  @Test
  void teacherCanCorrectGradeForCourseTheyTeach() {
    var teacher = saveTeacher();
    var student = saveStudent();
    var exam = saveExam(saveAssignment(teacher));
    var grade = saveGrade(exam, student, new BigDecimal("12.0"));

    var corrected =
        webTestClient
            .put()
            .uri("/exams/{exam_id}/students/{student_id}/grade", exam.getId(), student.getId())
            .header("Authorization", "Bearer " + token(teacher))
            .bodyValue(
                GradeCorrectionRequest.builder()
                    .value(new BigDecimal("18.0"))
                    .reason("Late submission")
                    .build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(GradeResponse.class)
            .returnResult()
            .getResponseBody();
    assertNotNull(corrected);
    assertEquals(0, new BigDecimal("18.0").compareTo(corrected.value()));
    assertEquals(grade.getId(), corrected.id());
  }

  @Test
  void teacherCannotCorrectGradeForCourseTheyDoNotTeach() {
    var teacher = saveTeacher();
    var otherTeacher = saveTeacher();
    var student = saveStudent();
    var exam = saveExam(saveAssignment(otherTeacher));
    saveGrade(exam, student, new BigDecimal("12.0"));

    webTestClient
        .put()
        .uri("/exams/{exam_id}/students/{student_id}/grade", exam.getId(), student.getId())
        .header("Authorization", "Bearer " + token(teacher))
        .bodyValue(
            GradeCorrectionRequest.builder()
                .value(new BigDecimal("18.0"))
                .reason("Recheck")
                .build())
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void studentCannotCorrectGrade() {
    var student = saveStudent();
    var exam = saveExam(saveAssignment(saveTeacher()));
    saveGrade(exam, student, new BigDecimal("12.0"));

    webTestClient
        .put()
        .uri("/exams/{exam_id}/students/{student_id}/grade", exam.getId(), student.getId())
        .header("Authorization", "Bearer " + token(student))
        .bodyValue(
            GradeCorrectionRequest.builder()
                .value(new BigDecimal("18.0"))
                .reason("Recheck")
                .build())
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void cannotCorrectGradeThatDoesNotExist() {
    var admin = saveAdmin();
    var student = saveStudent();
    var exam = saveExam(saveAssignment(saveTeacher()));

    webTestClient
        .put()
        .uri("/exams/{exam_id}/students/{student_id}/grade", exam.getId(), student.getId())
        .header("Authorization", "Bearer " + token(admin))
        .bodyValue(
            GradeCorrectionRequest.builder()
                .value(new BigDecimal("18.0"))
                .reason("Recheck")
                .build())
        .exchange()
        .expectStatus()
        .isNotFound();
  }
}
