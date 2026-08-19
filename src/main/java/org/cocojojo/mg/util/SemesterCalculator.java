package org.cocojojo.mg.util;

import java.time.LocalDate;
import org.cocojojo.mg.model.enums.Semester;

public final class SemesterCalculator {

  private SemesterCalculator() {}

  public static Semester semesterFor(int entryYear, LocalDate date) {
    var month = date.getMonthValue();
    var academicYearStart = month >= 9 ? date.getYear() : date.getYear() - 1;
    var offset = academicYearStart - entryYear;
    var ordinal = (month >= 9 ? 0 : 1) + offset * 2;
    var clamped = Math.max(0, Math.min(ordinal, Semester.values().length - 1));
    return Semester.values()[clamped];
  }

  public static int entryYearFor(Semester semester, LocalDate date) {
    var month = date.getMonthValue();
    var firstHalf = month >= 9;
    var academicYearStart = firstHalf ? date.getYear() : date.getYear() - 1;
    var offset = firstHalf ? (semester.ordinal() + 1) / 2 : semester.ordinal() / 2;
    return academicYearStart - offset;
  }
}
