package org.cocojojo.mg.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.cocojojo.mg.model.Teacher;
import org.cocojojo.mg.repository.model.JTeacher;
import org.junit.jupiter.api.Test;

class TeacherMapperTest {

  private final TeacherMapper mapper = new TeacherMapper();
  private final UUID id = UUID.randomUUID();

  @Test
  void toModel_maps_every_field_including_password() {
    var entity =
        JTeacher.builder()
            .id(id)
            .firstname("Grace")
            .lastname("Hopper")
            .email("grace@hei.school")
            .password("secret")
            .build();

    var model = mapper.toModel(entity);

    assertEquals(id, model.id());
    assertEquals("Grace", model.firstname());
    assertEquals("Hopper", model.lastname());
    assertEquals("grace@hei.school", model.email());
    assertEquals("secret", model.password());
  }

  @Test
  void toEntity_round_trips_model() {
    var model =
        Teacher.builder()
            .id(id)
            .firstname("Grace")
            .lastname("Hopper")
            .email("grace@hei.school")
            .password("secret")
            .build();

    var entity = mapper.toEntity(model);

    assertEquals(id, entity.getId());
    assertEquals("Grace", entity.getFirstname());
    assertEquals("Hopper", entity.getLastname());
    assertEquals("grace@hei.school", entity.getEmail());
    assertEquals("secret", entity.getPassword());
  }

  @Test
  void toResponse_maps_public_fields_only() {
    var model =
        Teacher.builder()
            .id(id)
            .firstname("Grace")
            .lastname("Hopper")
            .email("grace@hei.school")
            .password("secret")
            .build();

    var response = mapper.toResponse(model);

    assertEquals(id, response.id());
    assertEquals("Grace", response.firstname());
    assertEquals("Hopper", response.lastname());
    assertEquals("grace@hei.school", response.email());
  }
}
