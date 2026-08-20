package org.cocojojo.mg.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.model.Promotion;
import org.cocojojo.mg.model.Student;
import org.cocojojo.mg.model.StudentSummary;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudentMapperTest {

  private final UUID studentId = UUID.randomUUID();
  private final UUID promotionId = UUID.randomUUID();
  private final UUID groupId = UUID.randomUUID();

  private final StudentMapper mapper = new StudentMapper(new PromotionMapper());

  private JStudent entity;
  private Group currentGroup;

  @BeforeEach
  void setUp() {
    var jPromotion =
        JPromotion.builder().id(promotionId).ref("P1").name("Promotion").entryYear(2023).build();
    entity =
        JStudent.builder()
            .id(studentId)
            .firstname("Alan")
            .lastname("Turing")
            .email("alan@hei.school")
            .password("secret")
            .std("STD-01")
            .promotion(jPromotion)
            .build();
    var promotion =
        Promotion.builder().id(promotionId).ref("P1").name("Promotion").entryYear(2023).build();
    currentGroup = Group.builder().id(groupId).ref("G1").promotion(promotion).build();
  }

  @Test
  void toSummary_maps_identity_fields_without_password() {
    var summary = mapper.toSummary(entity);

    assertEquals(studentId, summary.id());
    assertEquals("Alan", summary.firstname());
    assertEquals("Turing", summary.lastname());
    assertEquals("alan@hei.school", summary.email());
    assertEquals("STD-01", summary.std());
  }

  @Test
  void toModel_maps_every_field_with_current_group() {
    var model = mapper.toModel(entity, currentGroup);

    assertEquals(studentId, model.id());
    assertEquals("Alan", model.firstname());
    assertEquals("Turing", model.lastname());
    assertEquals("alan@hei.school", model.email());
    assertEquals("secret", model.password());
    assertEquals("STD-01", model.std());
    assertEquals(promotionId, model.promotion().id());
    assertEquals(groupId, model.currentGroup().id());
  }

  @Test
  void toModel_accepts_null_current_group() {
    var model = mapper.toModel(entity, null);

    assertEquals(studentId, model.id());
    assertNull(model.currentGroup());
    assertEquals("STD-01", model.std());
  }

  @Test
  void toResponse_maps_public_fields_only() {
    var model =
        Student.builder()
            .id(studentId)
            .firstname("Alan")
            .lastname("Turing")
            .email("alan@hei.school")
            .password("secret")
            .std("STD-01")
            .promotion(
                Promotion.builder()
                    .id(promotionId)
                    .ref("P1")
                    .name("Promotion")
                    .entryYear(2023)
                    .build())
            .currentGroup(currentGroup)
            .build();

    var response = mapper.toResponse(model);

    assertEquals(studentId, response.id());
    assertEquals("STD-01", response.std());
    assertEquals("Alan", response.firstname());
    assertEquals("Turing", response.lastname());
    assertEquals("alan@hei.school", response.email());
    assertEquals(promotionId, response.promotionId());
    assertEquals("Promotion", response.promotionName());
    assertEquals(groupId, response.currentGroupId());
    assertEquals("G1", response.currentGroupRef());
  }

  @Test
  void toResponse_without_group_leaks_no_group_fields() {
    var model =
        Student.builder()
            .id(studentId)
            .std("STD-01")
            .promotion(
                Promotion.builder()
                    .id(promotionId)
                    .ref("P1")
                    .name("Promotion")
                    .entryYear(2023)
                    .build())
            .currentGroup(null)
            .build();

    var response = mapper.toResponse(model);

    assertNull(response.currentGroupId());
    assertNull(response.currentGroupRef());
  }

  @Test
  void toSummary_is_a_student_summary_record() {
    var summary = mapper.toSummary(entity);

    assertEquals(StudentSummary.class, summary.getClass());
  }
}
