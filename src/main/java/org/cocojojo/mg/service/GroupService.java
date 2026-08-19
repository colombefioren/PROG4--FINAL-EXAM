package org.cocojojo.mg.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.GroupMapper;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.repository.GroupRepository;
import org.cocojojo.mg.repository.model.JGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupService {

  private final GroupRepository groupRepository;
  private final GroupMapper groupMapper;
  private final PromotionService promotionService;

  public Page<GroupResponse> getAll(UUID promotionId, Pageable pageable) {
    var groups =
        promotionId == null
            ? groupRepository.findAll(pageable)
            : groupRepository.findByPromotionId(promotionId, pageable);
    return groups.map(groupMapper::toModel).map(groupMapper::toResponse);
  }

  public JGroup getEntityOrThrow(UUID id) {
    return groupRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Group with id: " + id + " not found."));
  }

  public Group getById(UUID id) {
    return groupMapper.toModel(
        groupRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Group with id:" + id + " not found.")));
  }

  @Transactional
  public GroupResponse upsert(GroupRequest request) {
    var group = request.id() == null ? JGroup.builder().build() : getEntityOrThrow(request.id());
    group.setRef(request.ref().toUpperCase());
    group.setPromotion(promotionService.getEntityOrThrow(request.promotionId()));
    group.setTrack(request.track());

    var saved = groupRepository.save(group);

    return groupMapper.toResponse(groupMapper.toModel(saved));
  }
}
