package org.cocojojo.mg.endpoint.rest.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.AdminRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.AdminResponse;
import org.cocojojo.mg.service.AdminService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * Deliberately minimal: admins are provisioned out-of-band (direct DB/migration), never through
 * this API, so there is no create endpoint. An admin can only update their own profile
 * (validateIsSelf in AdminService), which is why the id comes from the path rather than the body.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admins")
public class AdminController {

  private final AdminService service;

  @PutMapping("/{id}")
  public AdminResponse update(@PathVariable UUID id, @Valid @RequestBody AdminRequest request) {
    return service.update(id, request);
  }
}
