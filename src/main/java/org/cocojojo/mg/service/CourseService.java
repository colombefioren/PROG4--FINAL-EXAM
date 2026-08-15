package org.cocojojo.mg.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.CourseMapper;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.repository.CourseRepository;
import org.cocojojo.mg.repository.model.JCourse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

  private final CourseRepository courseRepository;
  private final CourseMapper courseMapper;

  public List<CourseResponse> getAll() {
    return courseRepository.findAll().stream().map(courseMapper::toModel).map(courseMapper::toResponse).toList();
  }

  public CourseResponse getById(UUID id) {
    return courseMapper.toResponse(courseMapper.toModel(getEntityOrThrow(id)));
  }

  public JCourse getEntityOrThrow(UUID id) {
    return courseRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Course with id: " + id + " not found."));
  }

  @Transactional
  public CourseResponse upsert(CourseRequest request) {
    var course = request.id() == null ? JCourse.builder().build() : getEntityOrThrow(request.id());
    course.setName(request.name());
    course.setCode(request.code().toUpperCase());
    course.setTrack(request.track());
    course.setCredits(request.credits());
    course.setTotalHours(request.totalHours());
    course.setStudentLevel(request.studentLevel());

    var saved = courseRepository.save(course);

    return courseMapper.toResponse(courseMapper.toModel(saved));
  }

  public JCourse find(UUID id) {
    return courseRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Course with id:" + id + " not found."));
  }

  public List<JCourse> findByStudentLevelOrderByCodeAsc(StudentLevel studentLevel) {
    return courseRepository.findByStudentLevelOrderByCodeAsc(studentLevel);
  }
}