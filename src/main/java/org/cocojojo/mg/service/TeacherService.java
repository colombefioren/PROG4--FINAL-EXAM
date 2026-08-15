package org.cocojojo.mg.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JTeacher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeacherService {

  private final TeacherRepository teacherRepository;

  public JTeacher getById(UUID id) {
    return teacherRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Teacher with id:" + id + " not found."));
  }
}
