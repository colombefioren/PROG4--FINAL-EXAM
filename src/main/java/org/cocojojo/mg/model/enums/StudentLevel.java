package org.cocojojo.mg.model.enums;

import java.util.EnumSet;
import java.util.Set;

public enum StudentLevel {
  L1(Semester.S1, Semester.S2),
  L2(Semester.S3, Semester.S4),
  L3(Semester.S5, Semester.S6);

  private final Semester firstSemester;
  private final Semester secondSemester;

  StudentLevel(Semester firstSemester, Semester secondSemester) {
    this.firstSemester = firstSemester;
    this.secondSemester = secondSemester;
  }

  public Set<Semester> semesters() {
    return EnumSet.of(firstSemester, secondSemester);
  }

  public static StudentLevel of(Semester semester) {
    for (StudentLevel level : values()) {
      if (level.semesters().contains(semester)) {
        return level;
      }
    }
    throw new IllegalArgumentException("No level for semester " + semester);
  }
}