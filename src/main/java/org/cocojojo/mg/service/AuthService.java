package org.cocojojo.mg.service;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.security.AuthenticatedAccount;
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

  public AuthenticatedAccount findByEmail(String email) {
    return adminRepository
        .findByEmailIgnoreCase(email)
        .map(
            a ->
                AuthenticatedAccount.builder()
                    .id(a.getId())
                    .firstname(a.getFirstname())
                    .lastname(a.getLastname())
                    .email(a.getEmail())
                    .password(a.getPassword())
                    .enabled(a.isEnabled())
                    .role(Role.ADMIN)
                    .build())
        .or(
            () ->
                teacherRepository
                    .findByEmailIgnoreCase(email)
                    .map(
                        t ->
                            AuthenticatedAccount.builder()
                                .id(t.getId())
                                .firstname(t.getFirstname())
                                .lastname(t.getLastname())
                                .email(t.getEmail())
                                .password(t.getPassword())
                                .enabled(t.isEnabled())
                                .role(Role.TEACHER)
                                .build()))
        .or(
            () ->
                studentRepository
                    .findByEmailIgnoreCase(email)
                    .map(
                        s ->
                            AuthenticatedAccount.builder()
                                .id(s.getId())
                                .firstname(s.getFirstname())
                                .lastname(s.getLastname())
                                .email(s.getEmail())
                                .password(s.getPassword())
                                .enabled(s.isEnabled())
                                .role(Role.STUDENT)
                                .build()))
        .orElseThrow(() -> new NoSuchElementException("No account found for email " + email));
  }
}
