package org.cocojojo.mg.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.cocojojo.mg.model.Course;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.model.JCourse;
import org.junit.jupiter.api.Test;

class CourseMapperTest {

  private final CourseMapper mapper = new CourseMapper();
  private final UUID id = UUID.randomUUID();

  @Test
  void toModel_maps_every_field() {
    var entity =
        JCourse.builder()
            .id(id)
            .code("TST1")
            .name("Test Course")
            .credits(6)
            .totalHours(30)
            .studentLevel(StudentLevel.L1)
            .track(Track.TN)
            .build();

    var model = mapper.toModel(entity);

    assertEquals(id, model.id());
    assertEquals("TST1", model.code());
    assertEquals("Test Course", model.name());
    assertEquals(6, model.credits());
    assertEquals(30, model.totalHours());
    assertEquals(StudentLevel.L1, model.studentLevel());
    assertEquals(Track.TN, model.track());
  }

  @Test
  void toModel_keeps_null_track() {
    var entity = JCourse.builder().studentLevel(StudentLevel.L1).track(null).build();

    var model = mapper.toModel(entity);

    assertNull(model.track());
  }

  @Test
  void toEntity_round_trips_model() {
    var model =
        Course.builder()
            .id(id)
            .code("TST1")
            .name("Test Course")
            .credits(6)
            .totalHours(30)
            .studentLevel(StudentLevel.L2)
            .track(Track.EL)
            .build();

    var entity = mapper.toEntity(model);

    assertEquals(id, entity.getId());
    assertEquals("TST1", entity.getCode());
    assertEquals("Test Course", entity.getName());
    assertEquals(6, entity.getCredits());
    assertEquals(30, entity.getTotalHours());
    assertEquals(StudentLevel.L2, entity.getStudentLevel());
    assertEquals(Track.EL, entity.getTrack());
  }

  @Test
  void toResponse_maps_every_field() {
    var model =
        Course.builder()
            .id(id)
            .code("TST1")
            .name("Test Course")
            .credits(6)
            .totalHours(30)
            .studentLevel(StudentLevel.L3)
            .track(null)
            .build();

    var response = mapper.toResponse(model);

    assertEquals(id, response.id());
    assertEquals("TST1", response.code());
    assertEquals("Test Course", response.name());
    assertEquals(6, response.credits());
    assertEquals(30, response.totalHours());
    assertEquals(StudentLevel.L3, response.studentLevel());
    assertNull(response.track());
  }

  @Test
  void toResponse_from_entity_uses_model_mapping() {
    var entity =
        JCourse.builder()
            .id(id)
            .code("TST1")
            .name("Test Course")
            .credits(2)
            .totalHours(15)
            .studentLevel(StudentLevel.L1)
            .track(Track.TN)
            .build();

    var response = mapper.toResponse(entity);

    assertEquals(id, response.id());
    assertEquals("TST1", response.code());
    assertEquals("Test Course", response.name());
    assertEquals(2, response.credits());
    assertEquals(15, response.totalHours());
    assertEquals(StudentLevel.L1, response.studentLevel());
    assertEquals(Track.TN, response.track());
  }
}
