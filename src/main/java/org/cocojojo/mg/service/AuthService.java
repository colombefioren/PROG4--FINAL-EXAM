package org.cocojojo.mg.service;

import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * HEI accounts are provisioned by an admin (students and teachers don't self-register), so this
 * service only ever issues tokens for existing accounts.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthResponse login(LoginRequest request) {
    var user =
        userRepository
            .findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new IllegalArgumentException("Invalid credentials");
    }

    var token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
    return AuthResponse.builder()
        .token(token)
        .userId(user.getId().toString())
        .role(user.getRole())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .build();
  }
}
