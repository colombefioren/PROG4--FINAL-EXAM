package org.cocojojo.mg.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.cocojojo.mg.model.Promotion;
import org.cocojojo.mg.repository.model.JPromotion;
import org.junit.jupiter.api.Test;

class PromotionMapperTest {

  private final PromotionMapper mapper = new PromotionMapper();
  private final UUID id = UUID.randomUUID();

  @Test
  void toModel_maps_every_field() {
    var entity =
        JPromotion.builder().id(id).ref("PROMO21").name("Twenty One").entryYear(2021).build();

    var model = mapper.toModel(entity);

    assertEquals(id, model.id());
    assertEquals("PROMO21", model.ref());
    assertEquals("Twenty One", model.name());
    assertEquals(2021, model.entryYear());
  }

  @Test
  void toEntity_round_trips_model() {
    var model =
        Promotion.builder().id(id).ref("PROMO22").name("Twenty Two").entryYear(2022).build();

    var entity = mapper.toEntity(model);

    assertEquals(id, entity.getId());
    assertEquals("PROMO22", entity.getRef());
    assertEquals("Twenty Two", entity.getName());
    assertEquals(2022, entity.getEntryYear());
  }

  @Test
  void toResponse_maps_every_field() {
    var model =
        Promotion.builder().id(id).ref("PROMO23").name("Twenty Three").entryYear(2023).build();

    var response = mapper.toResponse(model);

    assertEquals(id, response.id());
    assertEquals("PROMO23", response.ref());
    assertEquals("Twenty Three", response.name());
    assertEquals(2023, response.entryYear());
  }
}
