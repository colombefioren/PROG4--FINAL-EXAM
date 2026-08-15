package org.cocojojo.mg.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.PromotionMapper;
import org.cocojojo.mg.repository.PromotionRepository;
import org.cocojojo.mg.repository.model.JPromotion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromotionService {
  private final PromotionRepository repository;
  private final PromotionMapper mapper;

  public List<PromotionResponse> getAll() {
    return repository.findAll().stream().map(mapper::toModel).map(mapper::toResponse).toList();
  }

  public JPromotion getEntityOrThrow(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(
            () -> new ResourceNotFoundException("Promotion with id: " + id + " not found."));
  }

  public PromotionResponse getById(UUID id) {
    return mapper.toResponse(mapper.toModel(getEntityOrThrow(id)));
  }

  @Transactional
  public PromotionResponse upsert(PromotionRequest request) {
    var promotion =
        request.id() == null ? JPromotion.builder().build() : getEntityOrThrow(request.id());
    promotion.setRef(request.ref().toUpperCase());
    promotion.setName(request.name());
    promotion.setEntryYear(request.entryYear());

    var saved = repository.save(promotion);
    return mapper.toResponse(mapper.toModel(saved));
  }
}
