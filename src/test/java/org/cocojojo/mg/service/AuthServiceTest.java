package org.cocojojo.mg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.UiLoginResult;
import org.cocojojo.mg.endpoint.rest.controller.dto.UserResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.UnauthorizedException;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.mapper.UserMapper;
import org.cocojojo.mg.model.User;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.repository.UserRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.repository.model.JStudent;
import org.cocojojo.mg.repository.model.JTeacher;
import org.cocojojo.mg.repository.model.JUser;
import org.cocojojo.mg.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;
  @Mock private UserMapper userMapper;
  @Mock private SecurityUtil securityUtil;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private AuthService authService;

  private UUID userId;
  private LoginRequest request;
  private JUser user;
  private User model;
  private UserResponse userResponse;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    request = new LoginRequest("ada@hei.school", "secret");
    user = JAdmin.builder().id(userId).email("ada@hei.school").password("encoded").build();
    model = User.builder().id(userId).email("ada@hei.school").password("encoded").build();
    userResponse =
        UserResponse.builder()
            .id(userId)
            .firstname("Ada")
            .lastname("Lovelace")
            .email("ada@hei.school")
            .role(Role.ADMIN)
            .build();
  }

  @Test
  void login_returns_token_and_user_for_valid_credentials() {
    given(userRepository.findDeletedFlagByEmailIgnoreCase("ada@hei.school"))
        .willReturn(Optional.of(false));
    given(userRepository.findByEmailIgnoreCase("ada@hei.school")).willReturn(Optional.of(user));
    given(passwordEncoder.matches("secret", "encoded")).willReturn(true);
    given(securityUtil.getRoleFromUser(user)).willReturn(Role.ADMIN);
    given(jwtService.generateToken(userId, "ada@hei.school", Role.ADMIN)).willReturn("jwt-token");
    given(userMapper.toModel(user)).willReturn(model);
    given(userMapper.toResponse(model, Role.ADMIN)).willReturn(userResponse);

    var result = authService.login(request);

    assertEquals("jwt-token", result.token());
    assertEquals(userResponse, result.user());
  }

  @Test
  void login_respects_teacher_role() {
    var teacher = JTeacher.builder().id(userId).email("ada@hei.school").password("encoded").build();
    var teacherModel =
        User.builder().id(userId).email("ada@hei.school").password("encoded").build();
    var teacherResponse =
        UserResponse.builder()
            .id(userId)
            .firstname("Ada")
            .lastname("Lovelace")
            .email("ada@hei.school")
            .role(Role.TEACHER)
            .build();
    given(userRepository.findDeletedFlagByEmailIgnoreCase("ada@hei.school"))
        .willReturn(Optional.of(false));
    given(userRepository.findByEmailIgnoreCase("ada@hei.school")).willReturn(Optional.of(teacher));
    given(passwordEncoder.matches("secret", "encoded")).willReturn(true);
    given(securityUtil.getRoleFromUser(teacher)).willReturn(Role.TEACHER);
    given(jwtService.generateToken(userId, "ada@hei.school", Role.TEACHER)).willReturn("jwt-token");
    given(userMapper.toModel(teacher)).willReturn(teacherModel);
    given(userMapper.toResponse(teacherModel, Role.TEACHER)).willReturn(teacherResponse);

    var result = authService.login(request);

    assertEquals(Role.TEACHER, result.user().role());
  }

  @Test
  void login_respects_student_role() {
    var student = JStudent.builder().id(userId).email("ada@hei.school").password("encoded").build();
    var studentResponse =
        UserResponse.builder()
            .id(userId)
            .firstname("Ada")
            .lastname("Lovelace")
            .email("ada@hei.school")
            .role(Role.STUDENT)
            .build();
    given(userRepository.findDeletedFlagByEmailIgnoreCase("ada@hei.school"))
        .willReturn(Optional.of(false));
    given(userRepository.findByEmailIgnoreCase("ada@hei.school")).willReturn(Optional.of(student));
    given(passwordEncoder.matches("secret", "encoded")).willReturn(true);
    given(securityUtil.getRoleFromUser(student)).willReturn(Role.STUDENT);
    given(jwtService.generateToken(userId, "ada@hei.school", Role.STUDENT)).willReturn("jwt-token");
    given(userMapper.toModel(student)).willReturn(model);
    given(userMapper.toResponse(model, Role.STUDENT)).willReturn(studentResponse);

    var result = authService.login(request);

    assertEquals(Role.STUDENT, result.user().role());
  }

  @Test
  void login_throws_when_account_is_disabled() {
    given(userRepository.findDeletedFlagByEmailIgnoreCase("ada@hei.school"))
        .willReturn(Optional.of(true));

    var ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));

    assertEquals("This account has been disabled", ex.getMessage());
    then(userRepository)
        .should(never())
        .findByEmailIgnoreCase(org.mockito.ArgumentMatchers.anyString());
    then(jwtService)
        .should(never())
        .generateToken(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void login_throws_when_email_unknown() {
    given(userRepository.findDeletedFlagByEmailIgnoreCase("ada@hei.school"))
        .willReturn(Optional.of(false));
    given(userRepository.findByEmailIgnoreCase("ada@hei.school")).willReturn(Optional.empty());

    var ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));

    assertEquals("Invalid credentials", ex.getMessage());
    then(passwordEncoder)
        .should(never())
        .matches(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void login_throws_when_password_mismatch() {
    given(userRepository.findDeletedFlagByEmailIgnoreCase("ada@hei.school"))
        .willReturn(Optional.of(false));
    given(userRepository.findByEmailIgnoreCase("ada@hei.school")).willReturn(Optional.of(user));
    given(passwordEncoder.matches("secret", "encoded")).willReturn(false);

    var ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));

    assertEquals("Invalid credentials", ex.getMessage());
    then(jwtService)
        .should(never())
        .generateToken(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void login_throws_when_disabled_flag_absent_but_email_unknown() {
    given(userRepository.findDeletedFlagByEmailIgnoreCase("ada@hei.school"))
        .willReturn(Optional.empty());
    given(userRepository.findByEmailIgnoreCase("ada@hei.school")).willReturn(Optional.empty());

    assertThrows(UnauthorizedException.class, () -> authService.login(request));
  }

  @Test
  void loginForUi_redirects_admin_to_promotions() {
    given(userRepository.findDeletedFlagByEmailIgnoreCase("ada@hei.school"))
        .willReturn(Optional.of(false));
    given(userRepository.findByEmailIgnoreCase("ada@hei.school")).willReturn(Optional.of(user));
    given(passwordEncoder.matches("secret", "encoded")).willReturn(true);
    given(securityUtil.getRoleFromUser(user)).willReturn(Role.ADMIN);
    given(jwtService.generateToken(userId, "ada@hei.school", Role.ADMIN)).willReturn("jwt-token");
    given(userMapper.toModel(user)).willReturn(model);
    given(userMapper.toResponse(model, Role.ADMIN)).willReturn(userResponse);

    UiLoginResult result = authService.loginForUi(request);

    assertEquals("jwt-token", result.token());
    assertEquals("/ui/promotions", result.redirectUrl());
  }

  @Test
  void loginForUi_redirects_non_admin_to_forbidden() {
    var student = JStudent.builder().id(userId).email("ada@hei.school").password("encoded").build();
    var studentResponse =
        UserResponse.builder()
            .id(userId)
            .firstname("Ada")
            .lastname("Lovelace")
            .email("ada@hei.school")
            .role(Role.STUDENT)
            .build();
    given(userRepository.findDeletedFlagByEmailIgnoreCase("ada@hei.school"))
        .willReturn(Optional.of(false));
    given(userRepository.findByEmailIgnoreCase("ada@hei.school")).willReturn(Optional.of(student));
    given(passwordEncoder.matches("secret", "encoded")).willReturn(true);
    given(securityUtil.getRoleFromUser(student)).willReturn(Role.STUDENT);
    given(jwtService.generateToken(userId, "ada@hei.school", Role.STUDENT)).willReturn("jwt-token");
    given(userMapper.toModel(student)).willReturn(model);
    given(userMapper.toResponse(model, Role.STUDENT)).willReturn(studentResponse);

    UiLoginResult result = authService.loginForUi(request);

    assertEquals("/ui/forbidden", result.redirectUrl());
  }

  @Test
  void loginForUi_propagates_unauthorized_on_bad_credentials() {
    given(userRepository.findDeletedFlagByEmailIgnoreCase("ada@hei.school"))
        .willReturn(Optional.of(false));
    given(userRepository.findByEmailIgnoreCase("ada@hei.school")).willReturn(Optional.empty());

    assertThrows(UnauthorizedException.class, () -> authService.loginForUi(request));
  }
}
