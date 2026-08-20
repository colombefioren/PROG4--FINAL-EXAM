package org.cocojojo.mg.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.model.GroupFlow;
import org.cocojojo.mg.model.Promotion;
import org.cocojojo.mg.model.StudentSummary;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JGroupFlow;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupFlowMapperTest {

  private final UUID flowId = UUID.randomUUID();
  private final UUID studentId = UUID.randomUUID();
  private final UUID groupId = UUID.randomUUID();
  private final Instant createdAt = Instant.parse("2024-06-01T08:00:00Z");

  private final GroupFlowMapper mapper =
      new GroupFlowMapper(
          new StudentMapper(new PromotionMapper()), new GroupMapper(new PromotionMapper()));

  private JGroupFlow entity;

  @BeforeEach
  void setUp() {
    var jPromotion =
        JPromotion.builder()
            .id(UUID.randomUUID())
            .ref("P1")
            .name("Promotion")
            .entryYear(2023)
            .build();
    var jStudent =
        JStudent.builder()
            .id(studentId)
            .firstname("Alan")
            .lastname("Turing")
            .email("alan@hei.school")
            .std("STD-01")
            .promotion(jPromotion)
            .build();
    var jGroup = JGroup.builder().id(groupId).ref("G1").promotion(jPromotion).build();
    entity =
        JGroupFlow.builder()
            .id(flowId)
            .student(jStudent)
            .group(jGroup)
            .groupFlowType(GroupFlowType.JOIN)
            .createdAt(createdAt)
            .build();
  }

  @Test
  void toModel_maps_every_field() {
    var model = mapper.toModel(entity);

    assertEquals(flowId, model.id());
    assertEquals(studentId, model.student().id());
    assertEquals("STD-01", model.student().std());
    assertEquals(groupId, model.group().id());
    assertEquals("G1", model.group().ref());
    assertEquals(GroupFlowType.JOIN, model.groupFlowType());
    assertEquals(createdAt, model.createdAt());
  }

  @Test
  void toResponse_maps_summary_fields() {
    var model =
        GroupFlow.builder()
            .id(flowId)
            .student(
                StudentSummary.builder()
                    .id(studentId)
                    .firstname("Alan")
                    .lastname("Turing")
                    .email("alan@hei.school")
                    .std("STD-01")
                    .build())
            .group(
                Group.builder()
                    .id(groupId)
                    .ref("G1")
                    .promotion(
                        Promotion.builder()
                            .id(UUID.randomUUID())
                            .ref("P1")
                            .name("Promotion")
                            .entryYear(2023)
                            .build())
                    .build())
            .groupFlowType(GroupFlowType.JOIN)
            .createdAt(createdAt)
            .build();

    var response = mapper.toResponse(model);

    assertEquals(flowId, response.id());
    assertEquals(studentId, response.studentId());
    assertEquals("STD-01", response.studentStd());
    assertEquals(groupId, response.groupId());
    assertEquals("G1", response.groupRef());
    assertEquals(GroupFlowType.JOIN, response.groupFlowType());
    assertEquals(createdAt, response.createdAt());
  }
}
