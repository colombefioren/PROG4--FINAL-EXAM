package org.cocojojo.mg.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.cocojojo.mg.model.User;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.repository.model.JAdmin;
import org.junit.jupiter.api.Test;

class UserMapperTest {

  private final UserMapper mapper = new UserMapper();
  private final UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Test
  void toModel_maps_every_field() {
    var entity =
        JAdmin.builder()
            .id(id)
            .firstname("Ada")
            .lastname("Lovelace")
            .email("ada@hei.school")
            .password("secret")
            .build();

    var model = mapper.toModel(entity);

    assertEquals(id, model.id());
    assertEquals("Ada", model.firstname());
    assertEquals("Lovelace", model.lastname());
    assertEquals("ada@hei.school", model.email());
    assertEquals("secret", model.password());
  }

  @Test
  void toResponse_carries_the_given_role() {
    var model =
        User.builder()
            .id(id)
            .firstname("Ada")
            .lastname("Lovelace")
            .email("ada@hei.school")
            .password("secret")
            .build();

    var response = mapper.toResponse(model, Role.TEACHER);

    assertEquals(id, response.id());
    assertEquals("Ada", response.firstname());
    assertEquals("Lovelace", response.lastname());
    assertEquals("ada@hei.school", response.email());
    assertEquals(Role.TEACHER, response.role());
  }
}
