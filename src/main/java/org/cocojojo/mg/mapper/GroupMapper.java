package org.cocojojo.mg.mapper;

import lombok.AllArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupResponse;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.repository.model.JGroup;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class GroupMapper {

  private final PromotionMapper promotionMapper;

  public Group toModel(JGroup entity) {
    return Group.builder()
        .id(entity.getId())
        .ref(entity.getRef())
        .track(entity.getTrack())
        .promotion(promotionMapper.toModel(entity.getPromotion()))
        .build();
  }

  public GroupResponse toResponse(Group model) {
    return GroupResponse.builder()
        .id(model.id())
        .promotionId(model.promotion().id())
        .promotionName(model.promotion().name())
        .ref(model.ref())
        .track(model.track())
        .build();
  }
}
