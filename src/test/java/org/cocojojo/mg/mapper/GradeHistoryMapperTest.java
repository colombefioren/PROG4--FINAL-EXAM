package org.cocojojo.mg.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.model.Fraction;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JExam;
import org.cocojojo.mg.repository.model.JGrade;
import org.cocojojo.mg.repository.model.JGradeHistory;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.repository.model.JTeacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GradeHistoryMapperTest {

  private GradeHistoryMapper mapper;

  private final UUID id = UUID.randomUUID();
  private final UUID gradeId = UUID.randomUUID();
  private final UUID changedById = UUID.randomUUID();

  private JGrade grade;
  private JGradeHistory entity;

  @BeforeEach
  void setUp() {
    var promotionMapper = new PromotionMapper();
    var groupMapper = new GroupMapper(promotionMapper);
    var courseMapper = new CourseMapper();
    var teacherMapper = new TeacherMapper();
    var courseAssignmentMapper =
        new CourseAssignmentMapper(courseMapper, groupMapper, teacherMapper);
    var examMapper = new ExamMapper(courseAssignmentMapper);
    var studentMapper = new StudentMapper(promotionMapper);
    var gradeMapper = new GradeMapper(studentMapper, examMapper);
    mapper = new GradeHistoryMapper(gradeMapper, new UserMapper());

    var promotion =
        JPromotion.builder().id(UUID.randomUUID()).ref("2025").name("2025").entryYear(2024).build();
    var student =
        JStudent.builder()
            .id(UUID.randomUUID())
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
    var exam =
        JExam.builder()
            .id(UUID.randomUUID())
            .courseAssignment(assignment)
            .title("Midterm")
            .examDatetime(Instant.parse("2024-10-01T08:00:00Z"))
            .build();
    exam.setCoefficientFraction(new Fraction(1, 1));
    grade =
        JGrade.builder()
            .id(gradeId)
            .student(student)
            .exam(exam)
            .value(new BigDecimal("14.50"))
            .comment("Good")
            .build();
    entity =
        JGradeHistory.builder()
            .id(id)
            .grade(grade)
            .previousValue(new BigDecimal("12.00"))
            .newValue(new BigDecimal("14.50"))
            .reason("typo")
            .changedBy(
                JAdmin.builder().id(changedById).firstname("Ada").lastname("Lovelace").build())
            .changedAt(Instant.parse("2024-10-05T08:00:00Z"))
            .build();
  }

  @Test
  void toEntity_builds_the_entity() {
    var changedBy = JAdmin.builder().id(changedById).firstname("Ada").lastname("Lovelace").build();

    var result =
        mapper.toEntity(
            id, grade, new BigDecimal("12.00"), new BigDecimal("14.50"), "typo", changedBy);

    assertEquals(id, result.getId());
    assertEquals(grade, result.getGrade());
    assertEquals(new BigDecimal("12.00"), result.getPreviousValue());
    assertEquals(new BigDecimal("14.50"), result.getNewValue());
    assertEquals("typo", result.getReason());
    assertEquals(changedBy, result.getChangedBy());
  }

  @Test
  void toModel_maps_every_field() {
    var model = mapper.toModel(entity);

    assertEquals(id, model.id());
    assertEquals(gradeId, model.grade().id());
    assertEquals(new BigDecimal("12.00"), model.previousValue());
    assertEquals(new BigDecimal("14.50"), model.newValue());
    assertEquals("typo", model.reason());
    assertEquals(changedById, model.changedBy().id());
    assertEquals(Instant.parse("2024-10-05T08:00:00Z"), model.changedAt());
  }

  @Test
  void toResponse_builds_changed_by_display_name() {
    var response = mapper.toResponse(entity);

    assertEquals(id, response.id());
    assertEquals(gradeId, response.gradeId());
    assertEquals(new BigDecimal("12.00"), response.previousValue());
    assertEquals(new BigDecimal("14.50"), response.newValue());
    assertEquals("typo", response.reason());
    assertEquals(changedById, response.changedById());
    assertEquals("Ada Lovelace", response.changedByName());
    assertEquals(Instant.parse("2024-10-05T08:00:00Z"), response.changedAt());
  }
}
