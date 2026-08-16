package org.cocojojo.mg.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.GroupMapper;
import org.cocojojo.mg.repository.GroupRepository;
import org.cocojojo.mg.repository.model.JGroup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupService {
  private final GroupRepository repository;
  private final GroupMapper mapper;
  private final PromotionService promotionService;

  public List<GroupResponse> getAll(UUID promotionId) {
    var groups =
        promotionId == null ? repository.findAll() : repository.findByPromotionId(promotionId);
    return groups.stream().map(mapper::toModel).map(mapper::toResponse).toList();
  }

  public JGroup getEntityOrThrow(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Group with id: " + id + " not found."));
  }

  @Transactional
  public GroupResponse upsert(GroupRequest request) {
    var group = request.id() == null ? JGroup.builder().build() : getEntityOrThrow(request.id());
    group.setRef(request.ref().toUpperCase());
    group.setPromotion(promotionService.getEntityOrThrow(request.promotionId()));
    group.setTrack(request.track());

    var saved = repository.save(group);

    return mapper.toResponse(mapper.toModel(saved));
  }
  public JGroup getByIdOrThrow(UUID id) {
    return getEntityOrThrow(id);
  }
}
