package org.cocojojo.mg.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.CourseMapper;
import org.cocojojo.mg.mapper.PromotionMapper;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.PromotionRepository;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JPromotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromotionService {
  private final PromotionRepository repository;
  private final PromotionMapper mapper;
  private final CourseAssignmentRepository courseAssignmentRepository;
  private final CourseMapper courseMapper;

  public Page<PromotionResponse> getAll(Pageable pageable) {
    return repository.findAll(pageable).map(mapper::toModel).map(mapper::toResponse);
  }

  /**
   * The curriculum of a promotion: the distinct courses assigned to any of its groups, optionally
   * filtered by level and track. A course without a track (common) applies to every track.
   */
  public List<CourseResponse> getCourses(UUID promotionId, StudentLevel level, Track track) {
    getEntityOrThrow(promotionId);
    return courseAssignmentRepository.findCurriculumCoursesByPromotion(promotionId).stream()
        .filter(course -> level == null || course.getStudentLevel() == level)
        .filter(course -> track == null || course.getTrack() == null || course.getTrack() == track)
        .sorted(Comparator.comparing(JCourse::getCode))
        .map(courseMapper::toResponse)
        .toList();
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

  public List<PromotionResponse> getAllWithoutPagination() {
    return repository.findAll().stream().map(mapper::toModel).map(mapper::toResponse).toList();
  }
}
