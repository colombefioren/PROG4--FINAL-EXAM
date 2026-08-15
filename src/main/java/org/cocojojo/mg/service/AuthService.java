package org.cocojojo.mg.service;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.endpoint.rest.security.SecurityUtil;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AdminRepository adminRepository;
  private final TeacherRepository teacherRepository;
  private final StudentRepository studentRepository;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;
  private final SecurityUtil securityUtil;

  public AuthResponse findByEmail(String email) {
    return adminRepository
        .findByEmailIgnoreCase(email)
        .map(a -> toAuthResponse(a, Role.ADMIN))
        .or(
            () ->
                teacherRepository
                    .findByEmailIgnoreCase(email)
                    .map(t -> toAuthResponse(t, Role.TEACHER)))
        .or(
            () ->
                studentRepository
                    .findByEmailIgnoreCase(email)
                    .map(s -> toAuthResponse(s, Role.STUDENT)))
        .orElseThrow(() -> new NoSuchElementException("No account found for email " + email));
  }

  public AuthResponse login(LoginRequest request) {
    var user = findUser(request.email());
    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new IllegalArgumentException("Invalid credentials");
    }
    return toAuthResponse(user, securityUtil.getRole(user));
  }

  private JUser findUser(String email) {
    return adminRepository
        .findByEmailIgnoreCase(email)
        .<JUser>map(a -> a)
        .or(() -> teacherRepository.findByEmailIgnoreCase(email).map(t -> (JUser) t))
        .or(() -> studentRepository.findByEmailIgnoreCase(email).map(s -> (JUser) s))
        .orElseThrow(() -> new NoSuchElementException("No account found for email " + email));
  }

  private AuthResponse toAuthResponse(JUser user, Role role) {
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
