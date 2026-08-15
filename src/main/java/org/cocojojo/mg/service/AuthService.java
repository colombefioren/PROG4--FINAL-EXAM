package org.cocojojo.mg.service;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.UserResponse;
import org.cocojojo.mg.endpoint.rest.security.AuthenticatedAccount;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
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

  public AuthenticatedAccount findByEmail(String email) {
    return adminRepository
        .findByEmailIgnoreCase(email)
        .map(a -> account(a, Role.ADMIN))
        .or(() -> teacherRepository.findByEmailIgnoreCase(email).map(t -> account(t, Role.TEACHER)))
        .or(() -> studentRepository.findByEmailIgnoreCase(email).map(s -> account(s, Role.STUDENT)))
        .orElseThrow(() -> new NoSuchElementException("No account found for email " + email));
  }

  public AuthResponse login(LoginRequest request) {
    var account = findByEmail(request.email());
    if (!account.enabled()) {
      throw new IllegalStateException("This account has been disabled");
    }
    if (!passwordEncoder.matches(request.password(), account.password())) {
      throw new IllegalArgumentException("Invalid credentials");
    }

    var token = jwtService.generateToken(account.id(), account.email(), account.role());
    var user =
        UserResponse.builder()
            .id(account.id())
            .firstname(account.firstname())
            .lastname(account.lastname())
            .email(account.email())
            .role(account.role())
            .build();
    return AuthResponse.builder().token(token).user(user).build();
  }

  private AuthenticatedAccount account(JUser user, Role role) {
    return AuthenticatedAccount.builder()
        .id(user.getId())
        .firstname(user.getFirstname())
        .lastname(user.getLastname())
        .email(user.getEmail())
        .password(user.getPassword())
        .enabled(user.isEnabled())
        .role(role)
        .build();
  }
}
