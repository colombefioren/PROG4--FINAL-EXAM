package org.cocojojo.mg.validator;

import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.repository.model.JUser;
import org.springframework.stereotype.Component;

@Component
public class GroupFlowValidator {

  public void validateIsStudent(JUser user) {
    if (!(user instanceof JStudent)) {
      throw new IllegalArgumentException("User " + user.getId() + " is not a student");
    }
  }
}
