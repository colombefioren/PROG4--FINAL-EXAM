package org.cocojojo.mg.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.cocojojo.mg.conf.FacadeIT;
import org.cocojojo.mg.endpoint.rest.controller.dto.GraduateResponse;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.CourseRepository;
import org.cocojojo.mg.repository.ExamRepository;
import org.cocojojo.mg.repository.GradeRepository;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.GroupRepository;
import org.cocojojo.mg.repository.PromotionRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JExam;
import org.cocojojo.mg.repository.model.JGrade;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JGroupFlow;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.repository.model.JTeacher;
import org.cocojojo.mg.service.GraduateListService;
import org.cocojojo.mg.service.ResultService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

class ResultsSummaryBatchIT extends FacadeIT {

  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  @Autowired private ResultService resultService;
  @Autowired private GraduateListService graduateListService;
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

  @AfterEach
  void tearDown() {
    jdbcTemplate.execute("delete from \"grade_history\"");
    jdbcTemplate.execute("delete from \"grade\"");
    examRepository.deleteAll();
    courseAssignmentRepository.deleteAll();
    groupFlowRepository.deleteAll();
    teacherRepository.deleteAll();
    studentRepository.deleteAll();
    courseRepository.deleteAll();
    groupRepository.deleteAll();
    promotionRepository.deleteAll();
  }

  private String unique(String prefix) {
    return "rb-" + prefix + SEQUENCE.incrementAndGet() + "@hei.school";
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
            .ref("RB-PROMO" + SEQUENCE.incrementAndGet())
            .name("RB Promotion " + SEQUENCE.incrementAndGet())
            .entryYear(2023)
            .build());
  }

  private JGroup saveGroup(JPromotion promotion, Track track) {
    return groupRepository.save(
        JGroup.builder()
            .promotion(promotion)
            .ref("RB-GRP" + SEQUENCE.incrementAndGet())
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
                .std("RB-STD" + SEQUENCE.incrementAndGet())
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

  private JCourse saveCourse(int credits, StudentLevel level) {
    return courseRepository.save(
        JCourse.builder()
            .code("RB-CODE" + SEQUENCE.incrementAndGet())
            .name("Course " + SEQUENCE.incrementAndGet())
            .credits(credits)
            .totalHours(30)
            .studentLevel(level)
            .track(level == StudentLevel.L1 ? null : Track.TN)
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

  private void saveGrade(JExam exam, JStudent student, BigDecimal value) {
    gradeRepository.save(
        JGrade.builder().exam(exam).student(student).value(value).comment("ok").build());
  }

  @Test
  void batchSummariesMatchPerStudentComputation() {
    var promotion = savePromotion();
    var groupTN = saveGroup(promotion, Track.TN);
    var groupEL = saveGroup(promotion, Track.EL);
    var graduate = saveStudent(promotion, groupTN);
    var failing = saveStudent(promotion, groupTN);
    var partial = saveStudent(promotion, groupEL);

    var tnL1 = saveAssignment(saveCourse(4, StudentLevel.L1), groupTN, 4, Semester.S1);
    var tnL2 = saveAssignment(saveCourse(4, StudentLevel.L2), groupTN, 4, Semester.S3);
    var tnL3 = saveAssignment(saveCourse(4, StudentLevel.L3), groupTN, 4, Semester.S5);
    var elL1 = saveAssignment(saveCourse(2, StudentLevel.L1), groupEL, 2, Semester.S1);
    var tnL1Exam = saveExam(tnL1);
    var tnL2Exam = saveExam(tnL2);
    var tnL3Exam = saveExam(tnL3);
    var elL1Exam = saveExam(elL1);

    saveGrade(tnL1Exam, graduate, new BigDecimal("14"));
    saveGrade(tnL2Exam, graduate, new BigDecimal("12"));
    saveGrade(tnL3Exam, graduate, new BigDecimal("13"));

    saveGrade(tnL1Exam, failing, new BigDecimal("14"));
    saveGrade(tnL2Exam, failing, new BigDecimal("8"));

    saveGrade(elL1Exam, partial, new BigDecimal("10"));

    var students = List.of(graduate, failing, partial);
    var summaries = resultService.computeResultsSummaries(students);

    for (var student : students) {
      assertEquals(
          resultService.computeResultsSummary(student.getId()), summaries.get(student.getId()));
    }
    assertTrue(summaries.get(graduate.getId()).graduate());
    assertFalse(summaries.get(failing.getId()).graduate());
    assertFalse(summaries.get(partial.getId()).graduate());
  }

  @Test
  void batchHandlesStudentWhoMovedBetweenGroups() {
    var promotion = savePromotion();
    var oldGroup = saveGroup(promotion, Track.TN);
    var newGroup = saveGroup(promotion, Track.EL);
    var student = saveStudent(promotion, oldGroup);

    var oldCourse = saveCourse(4, StudentLevel.L1);
    var oldAssignment = saveAssignment(oldCourse, oldGroup, 4, Semester.S1);
    saveGrade(saveExam(oldAssignment), student, new BigDecimal("14"));
    moveToGroup(student, newGroup);

    var newCourse = saveCourse(4, StudentLevel.L1);
    saveAssignment(newCourse, newGroup, 4, Semester.S1);

    var summaries = resultService.computeResultsSummaries(List.of(student));
    var perStudent = resultService.computeResultsSummary(student.getId());

    assertEquals(perStudent, summaries.get(student.getId()));

    var l1Courses = summaries.get(student.getId()).levels().get(0).courses();
    assertEquals(2, l1Courses.size());
    var oldResult =
        l1Courses.stream().filter(c -> c.courseId().equals(oldCourse.getId())).findFirst().get();
    var newResult =
        l1Courses.stream().filter(c -> c.courseId().equals(newCourse.getId())).findFirst().get();
    assertEquals(new BigDecimal("14.00"), oldResult.average());
    assertNull(newResult.average());
    assertFalse(summaries.get(student.getId()).graduate());
  }

  @Test
  void batchTracksMatchPerStudentTracks() {
    var promotion = savePromotion();
    var groupTN = saveGroup(promotion, Track.TN);
    var groupEL = saveGroup(promotion, Track.EL);
    var tnStudent = saveStudent(promotion, groupTN);
    var elStudent = saveStudent(promotion, groupEL);

    var tracks = resultService.currentTracks(List.of(tnStudent, elStudent));

    assertEquals(Track.TN, tracks.get(tnStudent.getId()));
    assertEquals(Track.EL, tracks.get(elStudent.getId()));
    assertEquals(resultService.currentTrack(tnStudent.getId()), tracks.get(tnStudent.getId()));
    assertEquals(resultService.currentTrack(elStudent.getId()), tracks.get(elStudent.getId()));
  }

  @Test
  void getGraduatesRanksGraduatesWithBatchComputation() {
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var graduate = saveStudent(promotion, group);
    var nonGraduate = saveStudent(promotion, group);

    var l1 = saveAssignment(saveCourse(4, StudentLevel.L1), group, 4, Semester.S1);
    var l2 = saveAssignment(saveCourse(4, StudentLevel.L2), group, 4, Semester.S3);
    var l3 = saveAssignment(saveCourse(4, StudentLevel.L3), group, 4, Semester.S5);

    var l1Exam = saveExam(l1);
    var l2Exam = saveExam(l2);
    var l3Exam = saveExam(l3);
    saveGrade(l1Exam, graduate, new BigDecimal("14"));
    saveGrade(l2Exam, graduate, new BigDecimal("12"));
    saveGrade(l3Exam, graduate, new BigDecimal("13"));
    saveGrade(l1Exam, nonGraduate, new BigDecimal("8"));

    var rows = graduateListService.getGraduates(promotion.getId());

    assertEquals(1, rows.size());
    GraduateResponse row = rows.get(0);
    assertEquals(1, row.rank());
    assertEquals(graduate.getStd(), row.std());
    assertEquals(Track.TN, row.track());
    assertEquals(new BigDecimal("13.00"), row.generalAverage());
  }

  @Test
  void batchRetakeUsesOnlyLatestAttempt() {
    var promotion = savePromotion();
    var group = saveGroup(promotion, Track.TN);
    var student = saveStudent(promotion, group);
    var course = saveCourse(4, StudentLevel.L1);

    var first = saveAssignment(course, group, 4, Semester.S1, 2023);
    var retake = saveAssignment(course, group, 6, Semester.S1, 2024);
    saveGrade(saveExam(first), student, new BigDecimal("8"));
    saveGrade(saveExam(retake), student, new BigDecimal("14"));

    var batch = resultService.computeResultsSummaries(List.of(student)).get(student.getId());
    var perStudent = resultService.computeResultsSummary(student.getId());

    assertEquals(perStudent, batch);
    var l1 =
        batch.levels().stream().filter(l -> l.level() == StudentLevel.L1).findFirst().orElseThrow();
    var courseResult = l1.courses().get(0);
    assertEquals(new BigDecimal("14.00"), courseResult.average());
    assertEquals(6, courseResult.credits());
    assertTrue(courseResult.passed());
  }

  @Test
  void batchTrackSwitchDropsCoursesOfTheLeftTrack() {
    var promotion = savePromotion();
    var tnGroup = saveGroup(promotion, Track.TN);
    var elGroup = saveGroup(promotion, Track.EL);
    var student = saveStudent(promotion, tnGroup);

    var l1Course = saveCourse(4, StudentLevel.L1);
    var elL2Course =
        courseRepository.save(
            JCourse.builder()
                .code("RB-CODE" + SEQUENCE.incrementAndGet())
                .name("EL L2 " + SEQUENCE.incrementAndGet())
                .credits(4)
                .totalHours(30)
                .studentLevel(StudentLevel.L2)
                .track(Track.EL)
                .build());
    saveGrade(
        saveExam(saveAssignment(l1Course, tnGroup, 4, Semester.S1)), student, new BigDecimal("14"));
    moveToGroup(student, elGroup);
    saveGrade(
        saveExam(saveAssignment(elL2Course, elGroup, 4, Semester.S3)),
        student,
        new BigDecimal("12"));
    // A failed TN L2 course must not appear in the EL student's curriculum.
    saveGrade(
        saveExam(saveAssignment(saveCourse(4, StudentLevel.L2), tnGroup, 4, Semester.S3)),
        student,
        new BigDecimal("8"));

    var batch = resultService.computeResultsSummaries(List.of(student)).get(student.getId());
    var perStudent = resultService.computeResultsSummary(student.getId());

    assertEquals(perStudent, batch);
    var l2 =
        batch.levels().stream().filter(l -> l.level() == StudentLevel.L2).findFirst().orElseThrow();
    assertEquals(1, l2.courses().size());
    assertEquals(Track.EL, l2.courses().get(0).track());
  }
}
