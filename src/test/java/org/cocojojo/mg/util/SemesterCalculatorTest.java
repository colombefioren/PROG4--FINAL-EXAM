package org.cocojojo.mg.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.cocojojo.mg.model.enums.Semester;
import org.junit.jupiter.api.Test;

class SemesterCalculatorTest {

  @Test
  void semesterFor_starts_at_s1_in_october() {
    assertEquals(Semester.S1, SemesterCalculator.semesterFor(2024, LocalDate.of(2024, 10, 15)));
  }

  @Test
  void semesterFor_is_s2_in_spring_of_the_entry_year() {
    assertEquals(Semester.S2, SemesterCalculator.semesterFor(2024, LocalDate.of(2025, 2, 15)));
    assertEquals(Semester.S2, SemesterCalculator.semesterFor(2024, LocalDate.of(2025, 8, 15)));
  }

  @Test
  void semesterFor_moves_to_next_year_in_october() {
    assertEquals(Semester.S3, SemesterCalculator.semesterFor(2024, LocalDate.of(2025, 10, 15)));
    assertEquals(Semester.S4, SemesterCalculator.semesterFor(2024, LocalDate.of(2026, 3, 15)));
  }

  @Test
  void semesterFor_clamps_beyond_s6() {
    assertEquals(Semester.S6, SemesterCalculator.semesterFor(2020, LocalDate.of(2025, 3, 15)));
    assertEquals(Semester.S6, SemesterCalculator.semesterFor(2010, LocalDate.of(2025, 10, 15)));
  }

  @Test
  void semesterFor_clamps_before_s1() {
    assertEquals(Semester.S1, SemesterCalculator.semesterFor(2030, LocalDate.of(2025, 3, 15)));
    assertEquals(Semester.S1, SemesterCalculator.semesterFor(2028, LocalDate.of(2025, 10, 15)));
  }

  @Test
  void entryYearFor_returns_current_academic_year_in_spring() {
    var date = LocalDate.of(2025, 3, 15);

    assertEquals(2024, SemesterCalculator.entryYearFor(Semester.S1, date));
    assertEquals(2024, SemesterCalculator.entryYearFor(Semester.S2, date));
    assertEquals(2023, SemesterCalculator.entryYearFor(Semester.S3, date));
    assertEquals(2023, SemesterCalculator.entryYearFor(Semester.S4, date));
    assertEquals(2022, SemesterCalculator.entryYearFor(Semester.S5, date));
    assertEquals(2022, SemesterCalculator.entryYearFor(Semester.S6, date));
  }

  @Test
  void entryYearFor_returns_academic_year_start_in_fall() {
    var date = LocalDate.of(2024, 10, 15);

    assertEquals(2024, SemesterCalculator.entryYearFor(Semester.S1, date));
    assertEquals(2023, SemesterCalculator.entryYearFor(Semester.S2, date));
    assertEquals(2023, SemesterCalculator.entryYearFor(Semester.S3, date));
    assertEquals(2022, SemesterCalculator.entryYearFor(Semester.S4, date));
  }

  @Test
  void semesterFor_returns_s1_then_s2_within_the_entry_academic_year() {
    assertEquals(Semester.S1, SemesterCalculator.semesterFor(2024, LocalDate.of(2024, 9, 1)));
    assertEquals(Semester.S1, SemesterCalculator.semesterFor(2024, LocalDate.of(2024, 12, 31)));
    assertEquals(Semester.S2, SemesterCalculator.semesterFor(2024, LocalDate.of(2025, 1, 1)));
    assertEquals(Semester.S2, SemesterCalculator.semesterFor(2024, LocalDate.of(2025, 6, 30)));
  }
}
