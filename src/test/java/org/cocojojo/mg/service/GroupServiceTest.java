package org.cocojojo.mg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.GroupMapper;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.model.Promotion;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.GroupRepository;
import org.cocojojo.mg.repository.model.JGroup;
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
class GroupServiceTest {

  @Mock private GroupRepository groupRepository;
  @Mock private GroupMapper groupMapper;
  @Mock private PromotionService promotionService;

  @InjectMocks private GroupService service;

  private UUID id;
  private UUID promotionId;
  private JGroup jGroup;
  private Group group;
  private GroupResponse response;

  @BeforeEach
  void setUp() {
    id = UUID.randomUUID();
    promotionId = UUID.randomUUID();
    var jPromotion =
        JPromotion.builder().id(promotionId).ref("P1").name("Promotion").entryYear(2023).build();
    jGroup = JGroup.builder().id(id).ref("G1").track(Track.TN).promotion(jPromotion).build();
    group =
        Group.builder()
            .id(id)
            .ref("G1")
            .track(Track.TN)
            .promotion(
                Promotion.builder()
                    .id(promotionId)
                    .ref("P1")
                    .name("Promotion")
                    .entryYear(2023)
                    .build())
            .build();
    response =
        GroupResponse.builder()
            .id(id)
            .promotionId(promotionId)
            .promotionName("Promotion")
            .ref("G1")
            .track(Track.TN)
            .build();
  }

  @Test
  void getAll_without_promotion_uses_findAll() {
    given(groupRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(jGroup)));
    given(groupMapper.toModel(jGroup)).willReturn(group);
    given(groupMapper.toResponse(group)).willReturn(response);

    var page = service.getAll(null, Pageable.unpaged());

    assertEquals(1, page.getTotalElements());
    assertEquals(response, page.getContent().get(0));
  }

  @Test
  void getAll_with_promotion_uses_findByPromotionId() {
    given(groupRepository.findByPromotionId(promotionId, Pageable.unpaged()))
        .willReturn(new PageImpl<>(List.of(jGroup)));
    given(groupMapper.toModel(jGroup)).willReturn(group);
    given(groupMapper.toResponse(group)).willReturn(response);

    var page = service.getAll(promotionId, Pageable.unpaged());

    assertEquals(1, page.getTotalElements());
  }

  @Test
  void getEntityOrThrow_returns_found_group() {
    given(groupRepository.findById(id)).willReturn(Optional.of(jGroup));

    assertEquals(jGroup, service.getEntityOrThrow(id));
  }

  @Test
  void getEntityOrThrow_throws_when_missing() {
    given(groupRepository.findById(id)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.getEntityOrThrow(id));
  }

  @Test
  void getById_maps_model() {
    given(groupRepository.findById(id)).willReturn(Optional.of(jGroup));
    given(groupMapper.toModel(jGroup)).willReturn(group);

    assertEquals(group, service.getById(id));
  }

  @Test
  void getById_throws_when_missing() {
    given(groupRepository.findById(id)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> service.getById(id));
  }

  @Test
  void upsert_creates_new_group_with_uppercased_ref() {
    var request = GroupRequest.builder().promotionId(promotionId).ref("g1").track(Track.TN).build();
    given(promotionService.getEntityOrThrow(promotionId)).willReturn(jGroup.getPromotion());
    given(groupRepository.save(any(JGroup.class))).willReturn(jGroup);
    given(groupMapper.toModel(jGroup)).willReturn(group);
    given(groupMapper.toResponse(group)).willReturn(response);

    service.upsert(request);

    var captor = forClass(JGroup.class);
    then(groupRepository).should().save(captor.capture());
    assertEquals("G1", captor.getValue().getRef());
  }

  @Test
  void upsert_updates_existing_group() {
    var request =
        GroupRequest.builder().id(id).promotionId(promotionId).ref("G1").track(Track.EL).build();
    given(groupRepository.findById(id)).willReturn(Optional.of(jGroup));
    given(promotionService.getEntityOrThrow(promotionId)).willReturn(jGroup.getPromotion());
    given(groupRepository.save(jGroup)).willReturn(jGroup);
    given(groupMapper.toModel(jGroup)).willReturn(group);
    given(groupMapper.toResponse(group)).willReturn(response);

    service.upsert(request);

    then(groupRepository).should().save(jGroup);
  }
}
