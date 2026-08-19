package org.cocojojo.mg.validator;

import java.time.LocalDate;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.repository.model.JUser;
import org.cocojojo.mg.util.SemesterCalculator;
import org.springframework.stereotype.Component;

@Component
public class GroupFlowValidator {

  public void validateIsStudent(JUser user) {
    if (!(user instanceof JStudent)) {
      throw new IllegalArgumentException("User " + user.getId() + " is not a student");
    }
  }

  public void validateTrackGroupSwitch(JStudent student, JGroup group) {
    if (group.getTrack() == null) {
      return;
    }
    var semester =
        SemesterCalculator.semesterFor(student.getPromotion().getEntryYear(), LocalDate.now());
    if (semester.compareTo(Semester.S4) < 0) {
      throw new IllegalArgumentException(
          "A student can only be switched to a group with a track from semester S4 onward");
    }
  }
}
