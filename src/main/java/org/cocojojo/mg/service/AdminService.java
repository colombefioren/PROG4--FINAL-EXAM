package org.cocojojo.mg.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.AdminRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.AdminResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.AdminMapper;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.validator.AdminValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

  private final AdminRepository adminRepository;
  private final AdminMapper mapper;
  private final AdminValidator validator;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public AdminResponse update(UUID adminId, AdminRequest request) {
    validator.validateIsSelf(adminId);
    var entity =
        adminRepository
            .findById(adminId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Admin with id:" + adminId + " not found."));
    entity.setFirstname(request.firstname());
    entity.setLastname(request.lastname());
    entity.setEmail(request.email());
    if (request.password() != null && !request.password().isBlank()) {
      entity.setPassword(passwordEncoder.encode(request.password()));
    }
    return mapper.toResponse(mapper.toModel(entity));
  }
}
