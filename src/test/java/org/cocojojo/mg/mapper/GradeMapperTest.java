package org.cocojojo.mg.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.model.Fraction;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
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

class GradeMapperTest {

  private GradeMapper mapper;

  private final UUID id = UUID.fromString("88888888-8888-8888-8888-888888888888");
  private final UUID studentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private final UUID examId = UUID.fromString("77777777-7777-7777-7777-777777777777");

  private JStudent student;
  private JExam exam;
  private JGrade entity;

  @BeforeEach
  void setUp() {
    var promotionMapper = new PromotionMapper();
    var studentMapper = new StudentMapper(promotionMapper);
    var groupMapper = new GroupMapper(promotionMapper);
    var courseMapper = new CourseMapper();
    var teacherMapper = new TeacherMapper();
    var courseAssignmentMapper =
        new CourseAssignmentMapper(courseMapper, groupMapper, teacherMapper);
    var examMapper = new ExamMapper(courseAssignmentMapper);
    mapper = new GradeMapper(studentMapper, examMapper);

    var promotion =
        JPromotion.builder().id(UUID.randomUUID()).ref("2025").name("2025").entryYear(2024).build();
    student =
        JStudent.builder()
            .id(studentId)
            .firstname("Grace")
            .lastname("Hopper")
            .email("grace@hei.school")
            .password("secret")
            .std("STD24001")
            .promotion(promotion)
            .build();
    var course =
        JCourse.builder()
            .id(UUID.randomUUID())
            .code("ALG1")
            .name("Algorithms")
            .credits(6)
            .totalHours(45)
            .studentLevel(StudentLevel.L1)
            .track(Track.EL)
            .build();
    var group =
        JGroup.builder()
            .id(UUID.randomUUID())
            .ref("G1")
            .track(Track.EL)
            .promotion(promotion)
            .build();
    var teacher =
        JTeacher.builder()
            .id(UUID.randomUUID())
            .firstname("Ada")
            .lastname("Lovelace")
            .email("a@hei.school")
            .build();
    var assignment =
        JCourseAssignment.builder()
            .id(UUID.randomUUID())
            .course(course)
            .group(group)
            .teachers(List.of(teacher))
            .academicYear(2025)
            .semester(Semester.S1)
            .credits(6)
            .build();
    exam =
        JExam.builder()
            .id(examId)
            .courseAssignment(assignment)
            .title("Midterm")
            .examDatetime(Instant.parse("2024-10-01T08:00:00Z"))
            .build();
    exam.setCoefficientFraction(new Fraction(1, 1));
    entity =
        JGrade.builder()
            .id(id)
            .student(student)
            .exam(exam)
            .value(new BigDecimal("14.50"))
            .comment("Good")
            .build();
  }

  @Test
  void toEntity_builds_the_entity() {
    var result = mapper.toEntity(id, student, exam, new BigDecimal("14.50"), "Good");

    assertEquals(id, result.getId());
    assertEquals(student, result.getStudent());
    assertEquals(exam, result.getExam());
    assertEquals(new BigDecimal("14.50"), result.getValue());
    assertEquals("Good", result.getComment());
    assertNull(result.getCreatedAt());
  }

  @Test
  void toModel_maps_every_field() {
    var model = mapper.toModel(entity);

    assertEquals(id, model.id());
    assertEquals(studentId, model.student().id());
    assertEquals("STD24001", model.student().std());
    assertEquals(examId, model.exam().id());
    assertEquals("Midterm", model.exam().title());
    assertEquals("ALG1", model.exam().courseAssignment().course().code());
    assertEquals(new BigDecimal("14.50"), model.value());
    assertEquals("Good", model.comment());
  }

  @Test
  void toResponse_maps_nested_identifiers() {
    var response = mapper.toResponse(entity);

    assertEquals(id, response.id());
    assertEquals(studentId, response.studentId());
    assertEquals("STD24001", response.studentStd());
    assertEquals(examId, response.examId());
    assertEquals("Midterm", response.examTitle());
    assertEquals("ALG1", response.courseCode());
    assertEquals(new BigDecimal("14.50"), response.value());
    assertEquals("Good", response.comment());
  }

  @Test
  void toResponse_keeps_null_comment() {
    var e =
        JGrade.builder().id(id).student(student).exam(exam).value(new BigDecimal("10.00")).build();

    var response = mapper.toResponse(e);

    assertEquals(new BigDecimal("10.00"), response.value());
    assertNull(response.comment());
  }
}
