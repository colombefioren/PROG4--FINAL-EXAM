package org.cocojojo.mg.validator;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.util.SecurityUtil;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminValidator {

  private final SecurityUtil securityUtil;

  public void validateIsSelf(UUID adminId) {
    if (!securityUtil.getCurrentUserIdOrThrow().equals(adminId)) {
      throw new ForbiddenAccessException("Admins may only update their own profile");
    }
  }
}
