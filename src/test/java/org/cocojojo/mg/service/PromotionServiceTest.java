package org.cocojojo.mg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.CourseMapper;
import org.cocojojo.mg.mapper.PromotionMapper;
import org.cocojojo.mg.model.Promotion;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.PromotionRepository;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JPromotion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

  @Mock private PromotionRepository repository;
  @Mock private PromotionMapper mapper;
  @Mock private CourseAssignmentRepository courseAssignmentRepository;
  @Mock private CourseMapper courseMapper;

  @InjectMocks private PromotionService service;

  private UUID id;
  private JPromotion jPromotion;
  private Promotion promotion;
  private PromotionResponse response;

  @BeforeEach
  void setUp() {
    id = UUID.randomUUID();
    jPromotion = JPromotion.builder().id(id).ref("P1").name("Promotion").entryYear(2023).build();
    promotion = Promotion.builder().id(id).ref("P1").name("Promotion").entryYear(2023).build();
    response =
        PromotionResponse.builder().id(id).ref("P1").name("Promotion").entryYear(2023).build();
  }

  @Test
  void getAll_maps_paged_promotions() {
    given(repository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(jPromotion)));
    given(mapper.toModel(jPromotion)).willReturn(promotion);
    given(mapper.toResponse(promotion)).willReturn(response);

    var page = service.getAll(Pageable.unpaged());

    assertEquals(1, page.getTotalElements());
    assertEquals(response, page.getContent().get(0));
  }

  @Test
  void getEntityOrThrow_returns_found_promotion() {
    given(repository.findById(id)).willReturn(Optional.of(jPromotion));

    assertEquals(jPromotion, service.getEntityOrThrow(id));
  }

  @Test
  void getEntityOrThrow_throws_when_missing() {
    given(repository.findById(id)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.getEntityOrThrow(id));
  }

  @Test
  void getById_maps_promotion() {
    given(repository.findById(id)).willReturn(Optional.of(jPromotion));
    given(mapper.toModel(jPromotion)).willReturn(promotion);
    given(mapper.toResponse(promotion)).willReturn(response);

    assertEquals(response, service.getById(id));
  }

  @Test
  void upsert_creates_new_promotion_with_uppercased_ref() {
    var request = PromotionRequest.builder().ref("p2").name("Two").entryYear(2024).build();
    given(repository.save(any(JPromotion.class))).willReturn(jPromotion);
    given(mapper.toModel(jPromotion)).willReturn(promotion);
    given(mapper.toResponse(promotion)).willReturn(response);

    var result = service.upsert(request);

    assertEquals(response, result);
    then(repository).should().save(any(JPromotion.class));
  }

  @Test
  void upsert_updates_existing_promotion() {
    var request =
        PromotionRequest.builder().id(id).ref("P1").name("Promotion").entryYear(2023).build();
    given(repository.findById(id)).willReturn(Optional.of(jPromotion));
    given(repository.save(jPromotion)).willReturn(jPromotion);
    given(mapper.toModel(jPromotion)).willReturn(promotion);
    given(mapper.toResponse(promotion)).willReturn(response);

    var result = service.upsert(request);

    assertEquals(response, result);
    then(repository).should().save(jPromotion);
  }

  @Test
  void getAllWithoutPagination_returns_all_mapped() {
    given(repository.findAll()).willReturn(List.of(jPromotion));
    given(mapper.toModel(jPromotion)).willReturn(promotion);
    given(mapper.toResponse(promotion)).willReturn(response);

    var result = service.getAllWithoutPagination();

    assertEquals(1, result.size());
    assertEquals(response, result.get(0));
  }

  @Test
  void getCourses_filters_by_level_and_track_and_sorts_by_code() {
    var alg =
        JCourse.builder()
            .id(UUID.randomUUID())
            .code("ALG1")
            .studentLevel(StudentLevel.L1)
            .track(null)
            .build();
    var tnL2 =
        JCourse.builder()
            .id(UUID.randomUUID())
            .code("MB1")
            .studentLevel(StudentLevel.L2)
            .track(Track.TN)
            .build();
    var elL2 =
        JCourse.builder()
            .id(UUID.randomUUID())
            .code("ACC1")
            .studentLevel(StudentLevel.L2)
            .track(Track.EL)
            .build();
    given(repository.findById(id)).willReturn(Optional.of(jPromotion));
    given(courseAssignmentRepository.findCurriculumCoursesByPromotion(id))
        .willReturn(List.of(alg, tnL2, elL2));
    given(courseMapper.toResponse(tnL2)).willReturn(CourseResponse.builder().code("MB1").build());

    var result = service.getCourses(id, StudentLevel.L2, Track.TN);

    assertEquals(1, result.size());
    assertEquals("MB1", result.get(0).code());
  }

  @Test
  void getCourses_without_filters_returns_all_sorted() {
    var alg =
        JCourse.builder()
            .id(UUID.randomUUID())
            .code("ALG1")
            .studentLevel(StudentLevel.L1)
            .track(null)
            .build();
    var acc =
        JCourse.builder()
            .id(UUID.randomUUID())
            .code("ACC1")
            .studentLevel(StudentLevel.L2)
            .track(Track.EL)
            .build();
    given(repository.findById(id)).willReturn(Optional.of(jPromotion));
    given(courseAssignmentRepository.findCurriculumCoursesByPromotion(id))
        .willReturn(List.of(alg, acc));
    given(courseMapper.toResponse(alg)).willReturn(CourseResponse.builder().code("ALG1").build());
    given(courseMapper.toResponse(acc)).willReturn(CourseResponse.builder().code("ACC1").build());

    var result = service.getCourses(id, null, null);

    assertEquals(2, result.size());
    assertEquals("ACC1", result.get(0).code());
    assertEquals("ALG1", result.get(1).code());
  }
}
