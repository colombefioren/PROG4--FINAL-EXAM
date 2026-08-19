package org.cocojojo.mg.util;

import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.repository.model.JTeacher;
import org.cocojojo.mg.repository.model.JUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

  public Role getRoleFromUser(JUser user) {
    if (user instanceof JAdmin) return Role.ADMIN;
    if (user instanceof JStudent) return Role.STUDENT;
    if (user instanceof JTeacher) return Role.TEACHER;
    throw new IllegalStateException("Unknown user type: " + user.getClass());
  }

  public Authentication getAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  public boolean isAuthenticated() {
    var auth = getAuthentication();
    return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
  }

  public Optional<UUID> getCurrentUserId() {
    return Optional.ofNullable(getAuthentication())
        .filter(Authentication::isAuthenticated)
        .map(Authentication::getPrincipal)
        .map(Object::toString)
        .map(UUID::fromString);
  }

  public UUID getCurrentUserIdOrThrow() {
    return getCurrentUserId()
        .orElseThrow(() -> new IllegalStateException("User not authenticated"));
  }

  public Optional<Role> getCurrentRole() {
    return Optional.ofNullable(getAuthentication())
        .filter(Authentication::isAuthenticated)
        .map(Authentication::getAuthorities)
        .filter(authorities -> !authorities.isEmpty())
        .map(authorities -> authorities.iterator().next().getAuthority())
        .map(authority -> authority.replace("ROLE_", ""))
        .map(this::parseRole);
  }

  private Role parseRole(String name) {
    try {
      return Role.valueOf(name);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public Role getCurrentRoleOrThrow() {
    return getCurrentRole().orElseThrow(() -> new IllegalStateException("Role not found"));
  }

  public boolean isAdmin() {
    return getCurrentRole().map(role -> role == Role.ADMIN).orElse(false);
  }

  public boolean isTeacher() {
    return getCurrentRole().map(role -> role == Role.TEACHER).orElse(false);
  }

  public boolean isStudent() {
    return getCurrentRole().map(role -> role == Role.STUDENT).orElse(false);
  }

  public void requireSelf(UUID userId) {
    if (getCurrentUserIdOrThrow().equals(userId)) {
      return;
    }
    throw new ForbiddenAccessException("You may only access your own records");
  }

  public void requireSelfOrAdmin(UUID userId) {
    if (isAdmin()) {
      return;
    }
    requireSelf(userId);
  }

  public void requireSelfOrStaff(UUID userId) {
    if (isAdmin() || isTeacher()) {
      return;
    }
    requireSelf(userId);
  }
}
