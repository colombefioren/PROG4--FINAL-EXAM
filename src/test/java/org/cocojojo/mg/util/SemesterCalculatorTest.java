package org.cocojojo.mg.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.cocojojo.mg.model.enums.Semester;
import org.junit.jupiter.api.Test;

class SemesterCalculatorTest {

  @Test
  void mapsEntryYearAndDateToSemester() {
    assertEquals(Semester.S1, SemesterCalculator.semesterFor(2026, LocalDate.of(2026, 9, 15)));
    assertEquals(Semester.S2, SemesterCalculator.semesterFor(2026, LocalDate.of(2027, 2, 10)));
    assertEquals(Semester.S3, SemesterCalculator.semesterFor(2025, LocalDate.of(2026, 10, 5)));
    assertEquals(Semester.S4, SemesterCalculator.semesterFor(2024, LocalDate.of(2026, 8, 19)));
    assertEquals(Semester.S5, SemesterCalculator.semesterFor(2024, LocalDate.of(2026, 9, 15)));
    assertEquals(Semester.S6, SemesterCalculator.semesterFor(2024, LocalDate.of(2027, 2, 10)));
  }

  @Test
  void clampsOutOfRangeSemesters() {
    assertEquals(Semester.S1, SemesterCalculator.semesterFor(2030, LocalDate.of(2026, 8, 19)));
    assertEquals(Semester.S6, SemesterCalculator.semesterFor(2020, LocalDate.of(2026, 8, 19)));
  }

  @Test
  void entryYearForRecoversTheEarliestEntryYearReachingTheSemester() {
    assertEquals(2025, SemesterCalculator.entryYearFor(Semester.S2, LocalDate.of(2026, 8, 19)));
    assertEquals(2024, SemesterCalculator.entryYearFor(Semester.S4, LocalDate.of(2026, 8, 19)));
    assertEquals(2023, SemesterCalculator.entryYearFor(Semester.S6, LocalDate.of(2026, 8, 19)));
    assertEquals(2025, SemesterCalculator.entryYearFor(Semester.S3, LocalDate.of(2026, 9, 15)));
  }

  @Test
  void entryYearForReachingTheSemesterHoldsAgainstSemesterFor() {
    var date = LocalDate.now();
    for (var semester : Semester.values()) {
      var entryYear = SemesterCalculator.entryYearFor(semester, date);
      assertTrue(
          SemesterCalculator.semesterFor(entryYear, date).ordinal() >= semester.ordinal(),
          "entryYearFor must yield at least the requested semester for " + semester);
    }
  }
}
