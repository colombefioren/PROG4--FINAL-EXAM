package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.StudentResponse;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.GroupRepository;
import org.cocojojo.mg.repository.PromotionRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JAdmin;
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

class GroupStudentsIT extends FacadeIT {

  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  @Autowired private AdminRepository adminRepository;
  @Autowired private TeacherRepository teacherRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private PromotionRepository promotionRepository;
  @Autowired private GroupFlowRepository groupFlowRepository;
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
    groupRepository.deleteAll();
    promotionRepository.deleteAll();
  }

  private String unique(String prefix) {
    return "gs-" + prefix + SEQUENCE.incrementAndGet() + "@hei.school";
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
            .ref("GS-PROMO" + SEQUENCE.incrementAndGet())
            .name("GS Promotion " + SEQUENCE.incrementAndGet())
            .entryYear(2023)
            .build());
  }

  private JGroup saveGroup(JPromotion promotion) {
    return groupRepository.save(
        JGroup.builder()
            .promotion(promotion)
            .ref("GS-GRP" + SEQUENCE.incrementAndGet())
            .track(Track.TN)
            .build());
  }

  private JStudent saveStudent(JPromotion promotion, JGroup group, String firstname) {
    var student =
        studentRepository.save(
            JStudent.builder()
                .firstname(firstname)
                .lastname("Turing")
                .email(unique("student"))
                .password(passwordEncoder.encode("secret123"))
                .std("GS-STD" + SEQUENCE.incrementAndGet())
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

  private void moveToGroup(JStudent student, JGroup group) {
    groupFlowRepository.save(
        JGroupFlow.builder()
            .student(student)
            .group(group)
            .groupFlowType(GroupFlowType.JOIN)
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

  private List<StudentResponse> getStudents(String token, UUID groupId) {
    return webTestClient
        .get()
        .uri("/groups/{id}/students", groupId)
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(StudentResponse.class)
        .returnResult()
        .getResponseBody();
  }

  private Set<String> stds(List<StudentResponse> students) {
    return students.stream().map(StudentResponse::std).collect(Collectors.toSet());
  }

  @Test
  void adminCanListGroupStudents() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    saveStudent(promotion, group, "Alan");

    var students = getStudents(token(admin), group.getId());

    assertEquals(1, students.size());
    assertEquals("Alan", students.get(0).firstname());
  }

  @Test
  void teacherCanListGroupStudents() {
    var teacher = saveTeacher();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group, "Alan");

    var students = getStudents(token(teacher), group.getId());

    assertEquals(1, students.size());
    assertEquals(student.getId(), students.get(0).id());
  }

  @Test
  void studentCannotListGroupStudents() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group, "Alan");

    webTestClient
        .get()
        .uri("/groups/{id}/students", group.getId())
        .header("Authorization", "Bearer " + token(student))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void anonymousCannotListGroupStudents() {
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    saveStudent(promotion, group, "Alan");

    webTestClient
        .get()
        .uri("/groups/{id}/students", group.getId())
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void unknownGroupIsNotFound() {
    var admin = saveAdmin();

    webTestClient
        .get()
        .uri("/groups/{id}/students", UUID.randomUUID())
        .header("Authorization", "Bearer " + token(admin))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void emptyGroupReturnsEmptyList() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);

    var students = getStudents(token(admin), group.getId());

    assertTrue(students.isEmpty());
  }

  @Test
  void groupReturnsOnlyItsOwnStudents() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var groupA = saveGroup(promotion);
    var groupB = saveGroup(promotion);
    var studentA = saveStudent(promotion, groupA, "Alan");
    saveStudent(promotion, groupB, "Grace");

    var students = getStudents(token(admin), groupA.getId());

    assertEquals(1, students.size());
    assertEquals(studentA.getId(), students.get(0).id());
  }

  @Test
  void movedOutStudentIsNotListedAndAppearsInNewGroup() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var oldGroup = saveGroup(promotion);
    var newGroup = saveGroup(promotion);
    var student = saveStudent(promotion, oldGroup, "Alan");
    moveToGroup(student, newGroup);

    var oldStudents = getStudents(token(admin), oldGroup.getId());
    var newStudents = getStudents(token(admin), newGroup.getId());

    assertTrue(oldStudents.isEmpty());
    assertEquals(1, newStudents.size());
    assertEquals(student.getId(), newStudents.get(0).id());
  }

  @Test
  void studentsAreReturnedWithCurrentGroupInfo() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group, "Alan");

    var students = getStudents(token(admin), group.getId());

    assertEquals(group.getId(), students.get(0).currentGroupId());
    assertEquals(group.getRef(), students.get(0).currentGroupRef());
    assertEquals(promotion.getId(), students.get(0).promotionId());
    assertEquals(student.getEmail(), students.get(0).email());
    assertEquals(student.getStd(), students.get(0).std());
  }

  @Test
  void multipleStudentsInSameGroupAreAllListed() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var alan = saveStudent(promotion, group, "Alan");
    var grace = saveStudent(promotion, group, "Grace");
    var linus = saveStudent(promotion, group, "Linus");

    var students = getStudents(token(admin), group.getId());

    assertEquals(Set.of(alan.getStd(), grace.getStd(), linus.getStd()), stds(students));
  }

  @Test
  void studentJoinedTwiceInSameGroupIsListedOnce() {
    var admin = saveAdmin();
    var promotion = savePromotion();
    var group = saveGroup(promotion);
    var student = saveStudent(promotion, group, "Alan");
    moveToGroup(student, group);

    var students = getStudents(token(admin), group.getId());

    assertEquals(1, students.size());
    assertEquals(student.getId(), students.get(0).id());
  }
}
