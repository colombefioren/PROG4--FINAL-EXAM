package org.cocojojo.mg.service;

import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.UiLoginResult;
import org.cocojojo.mg.endpoint.rest.controller.exception.UnauthorizedException;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.mapper.UserMapper;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.repository.UserRepository;
import org.cocojojo.mg.util.SecurityUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final UserMapper userMapper;
  private final SecurityUtil securityUtil;
  private final PasswordEncoder passwordEncoder;

  public AuthResponse login(LoginRequest request) {
    if (userRepository.findDeletedFlagByEmailIgnoreCase(request.email()).orElse(false)) {
      throw new UnauthorizedException("This account has been disabled");
    }

    var user =
        userRepository
            .findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new UnauthorizedException("Invalid credentials");
    }

    var role = securityUtil.getRoleFromUser(user);
    var token = jwtService.generateToken(user.getId(), user.getEmail(), role);
    var userResponse = userMapper.toResponse(userMapper.toModel(user), role);

    return AuthResponse.builder().token(token).user(userResponse).build();
  }

  public UiLoginResult loginForUi(LoginRequest request) {
    var auth = login(request);
    return UiLoginResult.builder()
        .token(auth.token())
        .redirectUrl(auth.user().role() == Role.ADMIN ? "/ui/promotions" : "/ui/not-authorized")
        .build();
  }
}
