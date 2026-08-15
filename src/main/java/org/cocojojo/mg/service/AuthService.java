package org.cocojojo.mg.service;

import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.endpoint.rest.security.SecurityUtil;
import org.cocojojo.mg.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final SecurityUtil securityUtil;

  public AuthResponse login(LoginRequest request) {
    var user =
        userRepository
            .findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new IllegalArgumentException("Invalid credentials");
    }

    var role = securityUtil.getRole(user);
    var token = jwtService.generateToken(user.getId(), user.getEmail(), role);
    return AuthResponse.builder()
        .token(token)
        .userId(user.getId().toString())
        .role(role)
        .firstName(user.getFirstname())
        .lastName(user.getLastname())
        .build();
  }
}
