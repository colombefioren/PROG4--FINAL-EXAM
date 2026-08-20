package org.cocojojo.mg.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JTeacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExamMapperTest {

  private ExamMapper mapper;

  private final UUID id = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private final UUID courseAssignmentId = UUID.fromString("33333333-3333-3333-3333-333333333333");

  private JCourseAssignment assignment;
  private JExam entity;

  @BeforeEach
  void setUp() {
    var promotionMapper = new PromotionMapper();
    var groupMapper = new GroupMapper(promotionMapper);
    var courseMapper = new CourseMapper();
    var teacherMapper = new TeacherMapper();
    var courseAssignmentMapper =
        new CourseAssignmentMapper(courseMapper, groupMapper, teacherMapper);
    mapper = new ExamMapper(courseAssignmentMapper);

    var promotion =
        JPromotion.builder().id(UUID.randomUUID()).ref("2025").name("2025").entryYear(2024).build();
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
    assignment =
        JCourseAssignment.builder()
            .id(courseAssignmentId)
            .course(course)
            .group(group)
            .teachers(List.of(teacher))
            .academicYear(2025)
            .semester(Semester.S1)
            .credits(6)
            .build();
    entity =
        JExam.builder()
            .id(id)
            .courseAssignment(assignment)
            .title("Midterm")
            .examDatetime(Instant.parse("2024-10-01T08:00:00Z"))
            .build();
    entity.setCoefficientFraction(new Fraction(1, 2));
  }

  @Test
  void toEntity_builds_entity_with_coefficient() {
    var result =
        mapper.toEntity(
            id, assignment, "Midterm", Instant.parse("2024-10-01T08:00:00Z"), new Fraction(1, 2));

    assertEquals(id, result.getId());
    assertEquals(assignment, result.getCourseAssignment());
    assertEquals("Midterm", result.getTitle());
    assertEquals(Instant.parse("2024-10-01T08:00:00Z"), result.getExamDatetime());
    assertEquals(new Fraction(1, 2), result.getCoefficientFraction());
  }

  @Test
  void toModel_maps_every_field() {
    var model = mapper.toModel(entity);

    assertEquals(id, model.id());
    assertEquals(courseAssignmentId, model.courseAssignment().id());
    assertEquals("ALG1", model.courseAssignment().course().code());
    assertEquals("Midterm", model.title());
    assertEquals(Instant.parse("2024-10-01T08:00:00Z"), model.examDatetime());
    assertEquals(new Fraction(1, 2), model.coefficient());
  }

  @Test
  void toResponse_from_entity_matches_toResponse_from_model() {
    var model = mapper.toModel(entity);

    var fromEntity = mapper.toResponse(entity);
    var fromModel = mapper.toResponse(model);

    assertEquals(fromModel, fromEntity);
    assertEquals(id, fromEntity.id());
    assertEquals(courseAssignmentId, fromEntity.courseAssignmentId());
    assertEquals("Midterm", fromEntity.title());
    assertEquals(new Fraction(1, 2), fromEntity.coefficient());
  }
}
