package org.cocojojo.mg.mapper;

import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionResponse;
import org.cocojojo.mg.model.Promotion;
import org.cocojojo.mg.repository.model.JPromotion;
import org.springframework.stereotype.Component;

@Component
public class PromotionMapper {

  public Promotion toModel(JPromotion entity) {
    return Promotion.builder()
        .id(entity.getId())
        .ref(entity.getRef())
        .name(entity.getName())
        .entryYear(entity.getEntryYear())
        .build();
  }

  public JPromotion toEntity(Promotion model) {
    return JPromotion.builder()
        .id(model.id())
        .ref(model.ref())
        .name(model.name())
        .entryYear(model.entryYear())
        .build();
  }

  public PromotionResponse toResponse(Promotion model) {
    return PromotionResponse.builder()
        .id(model.id())
        .ref(model.ref())
        .name(model.name())
        .entryYear(model.entryYear())
        .build();
  }
}
