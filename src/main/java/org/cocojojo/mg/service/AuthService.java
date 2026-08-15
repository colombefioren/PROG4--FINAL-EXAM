package org.cocojojo.mg.service;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.UserResponse;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JUser;
import org.cocojojo.mg.util.SecurityUtil;
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

  public JUser findByEmail(String email) {
    return adminRepository
        .findByEmailIgnoreCase(email)
        .<JUser>map(a -> a)
        .or(() -> teacherRepository.findByEmailIgnoreCase(email).map(t -> (JUser) t))
        .or(() -> studentRepository.findByEmailIgnoreCase(email).map(s -> (JUser) s))
        .orElseThrow(() -> new NoSuchElementException("No account found for email " + email));
  }

  public AuthResponse login(LoginRequest request) {
    var user = findByEmail(request.email());
    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new IllegalArgumentException("Invalid credentials");
    }

    var role = securityUtil.getRole(user);
    var token = jwtService.generateToken(user.getId(), user.getEmail(), role);
    var userResponse =
        UserResponse.builder()
            .id(user.getId())
            .firstname(user.getFirstname())
            .lastname(user.getLastname())
            .email(user.getEmail())
            .role(role)
            .build();
    return AuthResponse.builder().token(token).user(userResponse).build();
  }
}
