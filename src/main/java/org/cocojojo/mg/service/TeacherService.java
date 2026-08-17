package org.cocojojo.mg.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.TeacherRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.TeacherResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.TeacherMapper;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JTeacher;
import org.cocojojo.mg.util.SecurityUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherService {
  private final TeacherRepository repository;
  private final TeacherMapper mapper;
  private final PasswordEncoder passwordEncoder;
  private final SecurityUtil securityUtil;

  public List<TeacherResponse> getAll() {
    return repository.findAll().stream().map(mapper::toModel).map(mapper::toResponse).toList();
  }

  public TeacherResponse getById(UUID id) {
    securityUtil.requireSelfOrStaff(id);
    return mapper.toResponse(mapper.toModel(getEntityOrThrow(id)));
  }

  public JTeacher getEntityOrThrow(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Teacher with id: " + id + " not found."));
  }

  @Transactional
  public TeacherResponse upsert(TeacherRequest request) {
    if (request.id() == null) {
      return create(request);
    }
    return update(request);
  }

  private TeacherResponse create(TeacherRequest request) {
    if (request.password() == null || request.password().isBlank()) {
      throw new IllegalArgumentException("password is required when creating a teacher");
    }

    var teacher =
        repository.save(
            JTeacher.builder()
                .firstname(request.firstname())
                .lastname(request.lastname())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build());

    return mapper.toResponse(mapper.toModel(teacher));
  }

  private TeacherResponse update(TeacherRequest request) {
    var teacher = getEntityOrThrow(request.id());
    teacher.setFirstname(request.firstname());
    teacher.setLastname(request.lastname());
    teacher.setEmail(request.email());
    if (request.password() != null && !request.password().isBlank()) {
      teacher.setPassword(passwordEncoder.encode(request.password()));
    }

    return mapper.toResponse(mapper.toModel(repository.save(teacher)));
  }
}
