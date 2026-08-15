package org.cocojojo.mg.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.CourseMapper;
import org.cocojojo.mg.repository.CourseRepository;
import org.cocojojo.mg.repository.model.JCourse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {
  private final CourseRepository repository;
  private final CourseMapper mapper;

  public List<CourseResponse> getAll() {
    return repository.findAll().stream().map(mapper::toModel).map(mapper::toResponse).toList();
  }

  public CourseResponse getById(UUID id) {
    return mapper.toResponse(mapper.toModel(getByEntityOrThrow(id)));
  }

  public JCourse getByEntityOrThrow(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Course with id: " + id + " not found."));
  }

  @Transactional
  public CourseResponse upsert(CourseRequest request) {
    var course =
        request.id() == null ? JCourse.builder().build() : getByEntityOrThrow(request.id());
    course.setName(request.name());
    course.setCode(request.code());
    course.setTrack(request.track());
    course.setCredits(request.credits());
    course.setTotalHours(request.totalHours());
    course.setStudentLevel(request.studentLevel());

    var saved = repository.save(course);

    return mapper.toResponse(mapper.toModel(saved));
  }
}
