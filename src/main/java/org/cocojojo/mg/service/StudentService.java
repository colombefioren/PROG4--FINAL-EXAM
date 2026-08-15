package org.cocojojo.mg.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JStudent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentService {

  private final StudentRepository studentRepository;
  private final GroupFlowRepository groupFlowRepository;

  public JStudent find(UUID id) {
    return studentRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Student with id:" + id + " not found."));
  }

  public JGroup findCurrentGroup(UUID studentId) {
    return groupFlowRepository
        .findFirstByStudentIdOrderByCreatedAtDesc(studentId)
        .filter(flow -> flow.getGroupFlowType() == GroupFlowType.JOIN)
        .map(flow -> flow.getGroup())
        .orElseThrow(
            () -> new ResourceNotFoundException("Student with id:" + studentId + " has no group"));
  }
}
