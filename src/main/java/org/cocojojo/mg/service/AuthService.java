package org.cocojojo.mg.service;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.UserResponse;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AdminRepository adminRepository;
  private final TeacherRepository teacherRepository;
  private final StudentRepository studentRepository;
  private final JwtService jwtService;

  public UserResponse findByEmail(String email) {
    return adminRepository
        .findByEmailIgnoreCase(email)
        .map(a -> userResponse(a, Role.ADMIN))
        .or(
            () ->
                teacherRepository
                    .findByEmailIgnoreCase(email)
                    .map(t -> userResponse(t, Role.TEACHER)))
        .or(
            () ->
                studentRepository
                    .findByEmailIgnoreCase(email)
                    .map(s -> userResponse(s, Role.STUDENT)))
        .orElseThrow(() -> new NoSuchElementException("No account found for email " + email));
  }

  public AuthResponse login(LoginRequest request) {
    var user = findByEmail(request.email());
    var token = jwtService.generateToken(user.id(), user.email(), user.role());
    return AuthResponse.builder().token(token).user(user).build();
  }

  private UserResponse userResponse(JUser user, Role role) {
    return UserResponse.builder()
        .id(user.getId())
        .firstname(user.getFirstname())
        .lastname(user.getLastname())
        .email(user.getEmail())
        .role(role)
        .build();
  }
}
