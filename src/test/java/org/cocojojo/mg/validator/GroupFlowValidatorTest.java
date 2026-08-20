package org.cocojojo.mg.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.UUID;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JPromotion;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.util.SemesterCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupFlowValidatorTest {

  private GroupFlowValidator validator;

  private final UUID studentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

  @BeforeEach
  void setUp() {
    validator = new GroupFlowValidator();
  }

  private JStudent student(int entryYear) {
    return JStudent.builder()
        .id(studentId)
        .firstname("Grace")
        .lastname("Hopper")
        .promotion(
            JPromotion.builder()
                .id(UUID.randomUUID())
                .ref("P")
                .name("P")
                .entryYear(entryYear)
                .build())
        .build();
  }

  @Test
  void validateIsStudent_accepts_a_student() {
    validator.validateIsStudent(student(2024));
  }

  @Test
  void validateIsStudent_rejects_an_admin() {
    var admin = JAdmin.builder().id(studentId).build();

    var ex = assertThrows(IllegalArgumentException.class, () -> validator.validateIsStudent(admin));

    assertEquals("User " + studentId + " is not a student", ex.getMessage());
  }

  @Test
  void validateTrackGroupSwitch_allows_when_group_has_no_track() {
    var group = JGroup.builder().id(UUID.randomUUID()).ref("G1").track(null).build();

    validator.validateTrackGroupSwitch(student(2024), group);
  }

  @Test
  void validateTrackGroupSwitch_throws_before_semester_s4() {
    int entryYear = SemesterCalculator.entryYearFor(Semester.S1, LocalDate.now());
    var group = JGroup.builder().id(UUID.randomUUID()).ref("G1").track(Track.EL).build();

    var ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateTrackGroupSwitch(student(entryYear), group));

    assertEquals(
        "A student can only be switched to a group with a track from semester S4 onward",
        ex.getMessage());
  }

  @Test
  void validateTrackGroupSwitch_allows_from_semester_s4_onward() {
    int entryYear = SemesterCalculator.entryYearFor(Semester.S4, LocalDate.now());
    var group = JGroup.builder().id(UUID.randomUUID()).ref("G1").track(Track.EL).build();

    assertDoesNotThrow(() -> validator.validateTrackGroupSwitch(student(entryYear), group));
  }
}
