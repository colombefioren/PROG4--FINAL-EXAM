package org.cocojojo.mg.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JTeacher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CourseAssignmentMapperTest {

  private CourseAssignmentMapper mapper;

  private final UUID id = UUID.randomUUID();
  private final UUID courseId = UUID.randomUUID();
  private final UUID groupId = UUID.randomUUID();
  private final UUID teacherId = UUID.randomUUID();

  private JPromotion promotion;
  private JCourse course;
  private JGroup group;
  private JTeacher teacher;
  private JCourseAssignment entity;

  @BeforeEach
  void setUp() {
    var promotionMapper = new PromotionMapper();
    var groupMapper = new GroupMapper(promotionMapper);
    mapper = new CourseAssignmentMapper(new CourseMapper(), groupMapper, new TeacherMapper());
    promotion =
        JPromotion.builder().id(UUID.randomUUID()).ref("2025").name("2025").entryYear(2024).build();
    course =
        JCourse.builder()
            .id(courseId)
            .code("ALG1")
            .name("Algorithms")
            .credits(6)
            .totalHours(45)
            .studentLevel(StudentLevel.L1)
            .track(Track.EL)
            .build();
    group = JGroup.builder().id(groupId).ref("G1").track(Track.EL).promotion(promotion).build();
    teacher =
        JTeacher.builder()
            .id(teacherId)
            .firstname("Ada")
            .lastname("Lovelace")
            .email("a@hei.school")
            .build();
    entity =
        JCourseAssignment.builder()
            .id(id)
            .course(course)
            .group(group)
            .teachers(List.of(teacher))
            .academicYear(2025)
            .semester(Semester.S1)
            .credits(6)
            .build();
  }

  @Test
  void toEntity_builds_the_entity() {
    var result = mapper.toEntity(id, course, group, List.of(teacher), 2025, Semester.S1, 6);

    assertEquals(id, result.getId());
    assertEquals(course, result.getCourse());
    assertEquals(group, result.getGroup());
    assertEquals(List.of(teacher), result.getTeachers());
    assertEquals(2025, result.getAcademicYear());
    assertEquals(Semester.S1, result.getSemester());
    assertEquals(6, result.getCredits());
  }

  @Test
  void toModel_maps_every_field() {
    var model = mapper.toModel(entity);

    assertEquals(id, model.id());
    assertEquals(courseId, model.course().id());
    assertEquals("ALG1", model.course().code());
    assertEquals(groupId, model.group().id());
    assertEquals("G1", model.group().ref());
    assertEquals("Ada", model.teachers().get(0).firstname());
    assertEquals(2025, model.academicYear());
    assertEquals(Semester.S1, model.semester());
    assertEquals(6, model.credits());
  }

  @Test
  void toResponse_maps_nested_identifiers() {
    var response = mapper.toResponse(entity);

    assertEquals(id, response.id());
    assertEquals(courseId, response.courseId());
    assertEquals("ALG1", response.courseCode());
    assertEquals("Algorithms", response.courseName());
    assertEquals(groupId, response.groupId());
    assertEquals("G1", response.groupRef());
    assertEquals(teacherId, response.teachers().get(0).id());
    assertEquals(2025, response.academicYear());
    assertEquals(Semester.S1, response.semester());
    assertEquals(6, response.credits());
  }

  @Test
  void toResponse_from_model_matches_toResponse_from_entity() {
    var model = mapper.toModel(entity);

    assertEquals(mapper.toResponse(entity), mapper.toResponse(model));
  }

  @Test
  void toResponse_with_null_track_group_keeps_null_track() {
    var nullTrackGroup =
        JGroup.builder().id(groupId).ref("G1").track(null).promotion(promotion).build();
    var nullTrackCourse =
        JCourse.builder()
            .id(courseId)
            .code("COM1")
            .name("Common")
            .credits(6)
            .totalHours(45)
            .studentLevel(StudentLevel.L1)
            .track(null)
            .build();
    var e =
        JCourseAssignment.builder()
            .id(id)
            .course(nullTrackCourse)
            .group(nullTrackGroup)
            .teachers(List.of(teacher))
            .academicYear(2025)
            .semester(Semester.S1)
            .credits(6)
            .build();

    var model = mapper.toModel(e);

    assertNull(model.course().track());
    assertNull(model.group().track());
  }
}
