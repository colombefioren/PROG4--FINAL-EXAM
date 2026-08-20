package org.cocojojo.mg.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.repository.model.JTeacher;
import org.cocojojo.mg.repository.model.JUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilTest {

  private final SecurityUtil util = new SecurityUtil();
  private final UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticate(String principal, String role) {
    var token =
        new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    SecurityContextHolder.getContext().setAuthentication(token);
  }

  @Test
  void getRoleFromUser_maps_each_user_type() {
    assertEquals(Role.ADMIN, util.getRoleFromUser(JAdmin.builder().id(userId).build()));
    assertEquals(Role.STUDENT, util.getRoleFromUser(JStudent.builder().id(userId).build()));
    assertEquals(Role.TEACHER, util.getRoleFromUser(JTeacher.builder().id(userId).build()));
  }

  @Test
  void getRoleFromUser_throws_for_unknown_user_type() {
    var unknown = JUser.builder().id(userId).build();

    var ex = assertThrows(IllegalStateException.class, () -> util.getRoleFromUser(unknown));

    assertEquals("Unknown user type: " + unknown.getClass(), ex.getMessage());
  }

  @Test
  void findCurrentUserId_is_empty_without_authentication() {
    assertTrue(util.findCurrentUserId().isEmpty());
  }

  @Test
  void findCurrentUserId_returns_principal_when_authenticated() {
    authenticate(userId.toString(), "ADMIN");

    assertEquals(userId, util.findCurrentUserId().orElseThrow());
  }

  @Test
  void findCurrentUserId_is_empty_when_authentication_not_authenticated() {
    var token = new UsernamePasswordAuthenticationToken(userId.toString(), null);
    SecurityContextHolder.getContext().setAuthentication(token);

    assertTrue(util.findCurrentUserId().isEmpty());
  }

  @Test
  void getCurrentUserId_returns_id_when_authenticated() {
    authenticate(userId.toString(), "ADMIN");

    assertEquals(userId, util.getCurrentUserId());
  }

  @Test
  void getCurrentUserId_throws_when_not_authenticated() {
    var ex = assertThrows(IllegalStateException.class, util::getCurrentUserId);

    assertEquals("User not authenticated", ex.getMessage());
  }

  @Test
  void findCurrentRole_returns_role_from_authority() {
    authenticate(userId.toString(), "TEACHER");

    assertEquals(Role.TEACHER, util.findCurrentRole().orElseThrow());
  }

  @Test
  void findCurrentRole_is_empty_without_authentication() {
    assertTrue(util.findCurrentRole().isEmpty());
  }

  @Test
  void findCurrentRole_is_empty_for_unknown_authority() {
    authenticate(userId.toString(), "BOGUS");

    assertTrue(util.findCurrentRole().isEmpty());
  }

  @Test
  void isAdmin_returns_true_only_for_admin() {
    authenticate(userId.toString(), "ADMIN");
    assertTrue(util.isAdmin());

    SecurityContextHolder.clearContext();
    authenticate(userId.toString(), "STUDENT");
    assertFalse(util.isAdmin());
  }

  @Test
  void isTeacher_returns_true_only_for_teacher() {
    authenticate(userId.toString(), "TEACHER");
    assertTrue(util.isTeacher());

    SecurityContextHolder.clearContext();
    authenticate(userId.toString(), "STUDENT");
    assertFalse(util.isTeacher());
  }

  @Test
  void isStudent_returns_true_only_for_student() {
    authenticate(userId.toString(), "STUDENT");
    assertTrue(util.isStudent());

    SecurityContextHolder.clearContext();
    authenticate(userId.toString(), "ADMIN");
    assertFalse(util.isStudent());
  }

  @Test
  void isAdmin_returns_false_when_not_authenticated() {
    assertFalse(util.isAdmin());
    assertFalse(util.isTeacher());
    assertFalse(util.isStudent());
  }

  @Test
  void requireSelf_passes_when_id_matches_principal() {
    authenticate(userId.toString(), "STUDENT");

    util.requireSelf(userId);
  }

  @Test
  void requireSelf_throws_when_id_differs() {
    authenticate(userId.toString(), "STUDENT");
    var other = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    var ex = assertThrows(ForbiddenAccessException.class, () -> util.requireSelf(other));

    assertEquals("You may only access your own records", ex.getMessage());
  }

  @Test
  void requireSelfOrAdmin_passes_for_admin_on_any_id() {
    authenticate(userId.toString(), "ADMIN");
    var other = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    util.requireSelfOrAdmin(other);
  }

  @Test
  void requireSelfOrAdmin_passes_for_matching_id() {
    authenticate(userId.toString(), "STUDENT");

    util.requireSelfOrAdmin(userId);
  }

  @Test
  void requireSelfOrAdmin_throws_for_student_on_other_id() {
    authenticate(userId.toString(), "STUDENT");
    var other = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    assertThrows(ForbiddenAccessException.class, () -> util.requireSelfOrAdmin(other));
  }

  @Test
  void requireSelfOrStaff_passes_for_teacher_on_any_id() {
    authenticate(userId.toString(), "TEACHER");
    var other = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    util.requireSelfOrStaff(other);
  }

  @Test
  void requireSelfOrStaff_passes_for_matching_student() {
    authenticate(userId.toString(), "STUDENT");

    util.requireSelfOrStaff(userId);
  }

  @Test
  void requireSelfOrStaff_throws_for_student_on_other_id() {
    authenticate(userId.toString(), "STUDENT");
    var other = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    assertThrows(ForbiddenAccessException.class, () -> util.requireSelfOrStaff(other));
  }
}
