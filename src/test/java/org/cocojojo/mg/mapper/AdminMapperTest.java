package org.cocojojo.mg.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.cocojojo.mg.model.Admin;
import org.cocojojo.mg.repository.model.JAdmin;
import org.junit.jupiter.api.Test;

class AdminMapperTest {

  private final AdminMapper mapper = new AdminMapper();
  private final UUID id = UUID.randomUUID();

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
  void toResponse_maps_public_fields_only() {
    var model =
        Admin.builder()
            .id(id)
            .firstname("Ada")
            .lastname("Lovelace")
            .email("ada@hei.school")
            .password("secret")
            .build();

    var response = mapper.toResponse(model);

    assertEquals(id, response.id());
    assertEquals("Ada", response.firstname());
    assertEquals("Lovelace", response.lastname());
    assertEquals("ada@hei.school", response.email());
  }
}
