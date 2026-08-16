package org.cocojojo.mg.service;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.GroupMapper;
import org.cocojojo.mg.mapper.StudentMapper;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.model.Student;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.model.JStudent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentService {

  private final StudentRepository studentRepository;
  private final GroupFlowRepository groupFlowRepository;
  private final GroupMapper groupMapper;
  private final StudentMapper studentMapper;

  public JStudent getByIdOrThrow(UUID id) {
    return studentRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Student with id:" + id + " not found."));
  }

  public Student getById(UUID id) {
    return studentMapper.toModel(getByIdOrThrow(id), currentGroupOf(id).orElse(null));
  }

  public Group getCurrentGroup(UUID studentId) {
    return currentGroupOf(studentId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Student with id:" + studentId + " has no group"));
  }

  private Optional<Group> currentGroupOf(UUID studentId) {
    return groupFlowRepository
        .findFirstByStudentIdOrderByCreatedAtDesc(studentId)
        .filter(flow -> flow.getGroupFlowType() == GroupFlowType.JOIN)
        .map(flow -> groupMapper.toModel(flow.getGroup()));
  }
}
