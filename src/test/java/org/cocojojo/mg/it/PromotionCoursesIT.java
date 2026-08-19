package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResponse;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.model.enums.Role;
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
import org.cocojojo.mg.repository.model.JCourseAssignment;
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

class PromotionCoursesIT extends FacadeIT {

  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  @Autowired private AdminRepository adminRepository;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private GroupFlowRepository groupFlowRepository;
  @Autowired private CourseAssignmentRepository courseAssignmentRepository;
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
    jdbcTemplate.execute("delete from \"exam\"");
    jdbcTemplate.execute("delete from \"course_assignment_teacher\"");
    jdbcTemplate.execute("delete from \"course_assignment\"");
    groupFlowRepository.deleteAll();
    teacherRepository.deleteAll();
    studentRepository.deleteAll();
    adminRepository.deleteAll();
    courseRepository.deleteAll();
    groupRepository.deleteAll();
    promotionRepository.deleteAll();
  }

  private String unique(String prefix) {
    return "pc-" + prefix + SEQUENCE.incrementAndGet() + "@hei.school";
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
            .ref("PC-PROMO" + SEQUENCE.incrementAndGet())
            .name("PC Promotion " + SEQUENCE.incrementAndGet())
            .entryYear(2023)
            .build());
  }

  private JGroup saveGroup(JPromotion promotion, Track track) {
    return groupRepository.save(
        JGroup.builder()
            .promotion(promotion)
            .ref("PC-GRP" + SEQUENCE.incrementAndGet())
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
                .std("PC-STD" + SEQUENCE.incrementAndGet())
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

  private JCourse saveCourse(String codePrefix, int credits, StudentLevel level, Track track) {
    return courseRepository.save(
        JCourse.builder()
            .code(codePrefix + SEQUENCE.incrementAndGet())
            .name("Course " + codePrefix)
            .credits(credits)
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
            .academicYear(2023)
            .semester(semester)
            .credits(course.getCredits())
            .build());
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

  private List<CourseResponse> getCourses(String token, UUID promotionId, String query) {
    return webTestClient
        .get()
        .uri("/promotions/{id}/courses" + (query == null ? "" : query), promotionId)
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(CourseResponse.class)
        .returnResult()
        .getResponseBody();
  }

  @Test
  void adminCanGetPromotionCourses() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var course = saveCourse("PROG", 4, StudentLevel.L1, Track.TN);
    saveAssignment(course, group, Semester.S1);

    var courses = getCourses(token(admin), promotion.getId(), null);

    assertEquals(1, courses.size());
    assertEquals(course.getCode(), courses.get(0).code());
  }

  @Test
  void teacherCanGetPromotionCourses() {
    var teacher = saveTeacher();
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var course = saveCourse("PROG", 4, StudentLevel.L1, Track.TN);
    saveAssignment(course, group, Semester.S1);

    var courses = getCourses(token(teacher), promotion.getId(), null);

    assertEquals(1, courses.size());
    assertEquals(course.getCode(), courses.get(0).code());
  }

  @Test
  void studentCannotGetPromotionCourses() {
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var student = saveStudent(promotion, group);

    webTestClient
        .get()
        .uri("/promotions/{id}/courses", promotion.getId())
        .header("Authorization", "Bearer " + token(student))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void anonymousCannotGetPromotionCourses() {
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var course = saveCourse("PROG", 4, StudentLevel.L1, Track.TN);
    saveAssignment(course, group, Semester.S1);

    webTestClient
        .get()
        .uri("/promotions/{id}/courses", promotion.getId())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void unknownPromotionIsNotFound() {
    var admin = saveAdmin();

    webTestClient
        .get()
        .uri("/promotions/{id}/courses", UUID.randomUUID())
        .header("Authorization", "Bearer " + token(admin))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void emptyPromotionReturnsEmptyList() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    saveGroup(promotion, Track.TN);

    var courses = getCourses(token(admin), promotion.getId(), null);

    assertTrue(courses.isEmpty());
  }

  @Test
  void returnsOnlyCoursesAssignedToThisPromotion() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var otherPromotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var otherGroup = saveGroup(otherPromotion, Track.TN);
    var mine = saveCourse("PROG", 4, StudentLevel.L1, Track.TN);
    saveAssignment(mine, group, Semester.S1);
    var foreign = saveCourse("SYS3", 4, StudentLevel.L1, Track.TN);
    saveAssignment(foreign, otherGroup, Semester.S1);

    var courses = getCourses(token(admin), promotion.getId(), null);

    assertEquals(1, courses.size());
    assertEquals(mine.getCode(), courses.get(0).code());
  }

  @Test
  void sameCourseAssignedToSeveralGroupsIsListedOnce() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var groupA = saveGroup(promotion, Track.TN);
    var groupB = saveGroup(promotion, Track.EL);
    var course = saveCourse("PROG", 4, StudentLevel.L1, Track.TN);
    saveAssignment(course, groupA, Semester.S1);
    saveAssignment(course, groupB, Semester.S1);

    var courses = getCourses(token(admin), promotion.getId(), null);

    assertEquals(1, courses.size());
    assertEquals(course.getCode(), courses.get(0).code());
  }

  @Test
  void filterByStudentLevelKeepsOnlyMatchingCourses() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var l1a = saveCourse("PROG", 4, StudentLevel.L1, Track.TN);
    var l1b = saveCourse("MATH", 4, StudentLevel.L1, Track.TN);
    saveAssignment(l1a, group, Semester.S1);
    saveAssignment(l1b, group, Semester.S2);
    var l2 = saveCourse("SYS3", 4, StudentLevel.L2, Track.TN);
    saveAssignment(l2, group, Semester.S3);

    var courses = getCourses(token(admin), promotion.getId(), "?studentLevel=L2");

    assertEquals(1, courses.size());
    assertEquals(l2.getCode(), courses.get(0).code());
  }

  @Test
  void filterByTrackKeepsCommonCoursesForEveryTrack() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var tn = saveCourse("PROG", 4, StudentLevel.L1, Track.TN);
    var common = saveCourse("COMM", 4, StudentLevel.L1, null);
    var el = saveCourse("ELEC", 4, StudentLevel.L1, Track.EL);
    saveAssignment(tn, group, Semester.S1);
    saveAssignment(common, group, Semester.S1);
    saveAssignment(el, group, Semester.S1);

    var courses = getCourses(token(admin), promotion.getId(), "?track=TN");

    assertEquals(2, courses.size());
    assertTrue(courses.stream().anyMatch(c -> c.code().equals(tn.getCode())));
    assertTrue(courses.stream().anyMatch(c -> c.code().equals(common.getCode())));
    assertTrue(courses.stream().noneMatch(c -> c.code().equals(el.getCode())));
  }

  @Test
  void coursesAreSortedByCode() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var zed = saveCourse("ZED", 4, StudentLevel.L1, Track.TN);
    var abc = saveCourse("ABC", 4, StudentLevel.L1, Track.TN);
    var mid = saveCourse("MID", 4, StudentLevel.L1, Track.TN);
    saveAssignment(zed, group, Semester.S1);
    saveAssignment(abc, group, Semester.S1);
    saveAssignment(mid, group, Semester.S1);

    var courses = getCourses(token(admin), promotion.getId(), null);

    
    assertEquals(
        List.of(abc.getCode(), mid.getCode(), zed.getCode()),
        courses.stream().map(CourseResponse::code).toList());
  }
}
