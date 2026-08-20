package org.cocojojo.mg.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.AdminRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.AdminResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.AdminMapper;
import org.cocojojo.mg.model.Admin;
import org.cocojojo.mg.repository.AdminRepository;
import org.cocojojo.mg.repository.model.JAdmin;
import org.cocojojo.mg.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

  @Mock private AdminRepository adminRepository;
  @Mock private AdminMapper mapper;
  @Mock private SecurityUtil securityUtil;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private AdminService adminService;

  private UUID adminId;
  private JAdmin entity;
  private Admin model;
  private AdminResponse response;

  @BeforeEach
  void setUp() {
    adminId = UUID.randomUUID();
    entity =
        JAdmin.builder()
            .id(adminId)
            .firstname("Ada")
            .lastname("Lovelace")
            .email("ada@hei.school")
            .password("encoded")
            .build();
    model =
        Admin.builder()
            .id(adminId)
            .firstname("Ada")
            .lastname("Lovelace")
            .email("ada@hei.school")
            .password("encoded")
            .build();
    response =
        AdminResponse.builder()
            .id(adminId)
            .firstname("Ada")
            .lastname("Lovelace")
            .email("ada@hei.school")
            .build();
  }

  @Test
  void getById_returns_response_when_admin_exists_and_is_self() {
    given(adminRepository.findById(adminId)).willReturn(Optional.of(entity));
    given(mapper.toModel(entity)).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    var result = adminService.getById(adminId);

    assertEquals(response, result);
    then(securityUtil).should().requireSelf(adminId);
  }

  @Test
  void getById_throws_forbidden_when_not_self() {
    doThrow(new ForbiddenAccessException("forbidden")).when(securityUtil).requireSelf(adminId);

    assertThrows(ForbiddenAccessException.class, () -> adminService.getById(adminId));
    then(adminRepository).should(never()).findById(adminId);
  }

  @Test
  void getById_throws_not_found_when_admin_missing() {
    given(adminRepository.findById(adminId)).willReturn(Optional.empty());

    var ex = assertThrows(ResourceNotFoundException.class, () -> adminService.getById(adminId));

    assertEquals("Admin with id:" + adminId + " not found.", ex.getMessage());
    then(mapper).should(never()).toResponse(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void update_updates_fields_and_returns_response() {
    var request =
        AdminRequest.builder()
            .firstname("Grace")
            .lastname("Hopper")
            .email("grace@hei.school")
            .password("newPass")
            .build();
    given(adminRepository.findById(adminId)).willReturn(Optional.of(entity));
    given(passwordEncoder.encode("newPass")).willReturn("encoded-new");
    var updated =
        JAdmin.builder()
            .id(adminId)
            .firstname("Grace")
            .lastname("Hopper")
            .email("grace@hei.school")
            .password("encoded-new")
            .build();
    given(adminRepository.save(entity)).willReturn(updated);
    given(mapper.toModel(updated)).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    var result = adminService.update(adminId, request);

    assertEquals(response, result);
    assertEquals("Grace", entity.getFirstname());
    assertEquals("Hopper", entity.getLastname());
    assertEquals("grace@hei.school", entity.getEmail());
    assertEquals("encoded-new", entity.getPassword());
  }

  @Test
  void update_does_not_change_password_when_blank() {
    var request =
        AdminRequest.builder()
            .firstname("Ada")
            .lastname("Lovelace")
            .email("ada@hei.school")
            .password("   ")
            .build();
    given(adminRepository.findById(adminId)).willReturn(Optional.of(entity));
    given(adminRepository.save(entity)).willReturn(entity);
    given(mapper.toModel(entity)).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    adminService.update(adminId, request);

    assertEquals("encoded", entity.getPassword());
    then(passwordEncoder).should(never()).encode(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void update_does_not_change_password_when_null() {
    var request =
        AdminRequest.builder()
            .firstname("Ada")
            .lastname("Lovelace")
            .email("ada@hei.school")
            .password(null)
            .build();
    given(adminRepository.findById(adminId)).willReturn(Optional.of(entity));
    given(adminRepository.save(entity)).willReturn(entity);
    given(mapper.toModel(entity)).willReturn(model);
    given(mapper.toResponse(model)).willReturn(response);

    adminService.update(adminId, request);

    assertEquals("encoded", entity.getPassword());
    then(passwordEncoder).should(never()).encode(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void update_throws_not_found_when_admin_missing() {
    var request =
        AdminRequest.builder()
            .firstname("Ada")
            .lastname("Lovelace")
            .email("ada@hei.school")
            .build();
    given(adminRepository.findById(adminId)).willReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> adminService.update(adminId, request));
    then(adminRepository).should(never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void update_throws_forbidden_when_not_self() {
    var request =
        AdminRequest.builder()
            .firstname("Ada")
            .lastname("Lovelace")
            .email("ada@hei.school")
            .build();
    doThrow(new ForbiddenAccessException("forbidden")).when(securityUtil).requireSelf(adminId);

    assertThrows(ForbiddenAccessException.class, () -> adminService.update(adminId, request));
    then(adminRepository).should(never()).findById(adminId);
  }
}
