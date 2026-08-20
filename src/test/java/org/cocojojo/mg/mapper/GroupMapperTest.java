package org.cocojojo.mg.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.model.Promotion;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JPromotion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupMapperTest {

  private final UUID groupId = UUID.randomUUID();
  private final UUID promotionId = UUID.randomUUID();

  private final GroupMapper mapper = new GroupMapper(new PromotionMapper());

  private JPromotion jPromotion;
  private JGroup jGroup;
  private Promotion promotion;
  private Group group;

  @BeforeEach
  void setUp() {
    jPromotion =
        JPromotion.builder().id(promotionId).ref("P1").name("Promotion").entryYear(2023).build();
    jGroup = JGroup.builder().id(groupId).ref("G1").track(Track.TN).promotion(jPromotion).build();
    promotion =
        Promotion.builder().id(promotionId).ref("P1").name("Promotion").entryYear(2023).build();
    group = Group.builder().id(groupId).ref("G1").track(Track.TN).promotion(promotion).build();
  }

  @Test
  void toModel_maps_every_field() {
    var model = mapper.toModel(jGroup);

    assertEquals(groupId, model.id());
    assertEquals("G1", model.ref());
    assertEquals(Track.TN, model.track());
    assertEquals(promotionId, model.promotion().id());
  }

  @Test
  void toEntity_round_trips_model() {
    var entity = mapper.toEntity(group);

    assertEquals(groupId, entity.getId());
    assertEquals("G1", entity.getRef());
    assertEquals(Track.TN, entity.getTrack());
    assertEquals(promotionId, entity.getPromotion().getId());
  }

  @Test
  void toResponse_maps_promotion_fields() {
    var response = mapper.toResponse(group);

    assertEquals(groupId, response.id());
    assertEquals(promotionId, response.promotionId());
    assertEquals("Promotion", response.promotionName());
    assertEquals("G1", response.ref());
    assertEquals(Track.TN, response.track());
  }
}
