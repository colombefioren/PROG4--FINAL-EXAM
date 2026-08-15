package org.cocojojo.mg.service;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.AuthResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.LoginRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.UserResponse;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.mapper.UserMapper;
import org.cocojojo.mg.model.enums.Role;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.TeacherRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AdminRepository adminRepository;
  private final TeacherRepository teacherRepository;
  private final StudentRepository studentRepository;
  private final JwtService jwtService;
  private final UserMapper userMapper;

  public UserResponse findByEmail(String email) {
    return adminRepository
        .findByEmailIgnoreCase(email)
        .map(a -> userMapper.toResponse(userMapper.toModel(a), Role.ADMIN))
        .or(
            () ->
                teacherRepository
                    .findByEmailIgnoreCase(email)
                    .map(t -> userMapper.toResponse(userMapper.toModel(t), Role.TEACHER)))
        .or(
            () ->
                studentRepository
                    .findByEmailIgnoreCase(email)
                    .map(s -> userMapper.toResponse(userMapper.toModel(s), Role.STUDENT)))
        .orElseThrow(() -> new NoSuchElementException("No account found for email " + email));
  }

  public AuthResponse login(LoginRequest request) {
    var user = findByEmail(request.email());
    var token = jwtService.generateToken(user.id(), user.email(), user.role());
    return AuthResponse.builder().token(token).user(user).build();
  }
}
