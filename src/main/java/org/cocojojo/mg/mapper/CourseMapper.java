package org.cocojojo.mg.mapper;

import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResponse;
import org.cocojojo.mg.model.Course;
import org.cocojojo.mg.repository.model.JCourse;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

  public Course toModel(JCourse entity) {
    return Course.builder()
        .id(entity.getId())
        .code(entity.getCode())
        .name(entity.getName())
        .credits(entity.getCredits())
        .totalHours(entity.getTotalHours())
        .studentLevel(entity.getStudentLevel())
        .track(entity.getTrack())
        .build();
  }

  public JCourse toEntity(Course model) {
    return JCourse.builder()
        .id(model.id())
        .code(model.code())
        .name(model.name())
        .credits(model.credits())
        .totalHours(model.totalHours())
        .studentLevel(model.studentLevel())
        .track(model.track())
        .build();
  }

  public CourseResponse toResponse(JCourse entity) {
    return toResponse(toModel(entity));
  }

  public CourseResponse toResponse(Course model) {
    return CourseResponse.builder()
        .id(model.id())
        .code(model.code())
        .name(model.name())
        .credits(model.credits())
        .totalHours(model.totalHours())
        .studentLevel(model.studentLevel())
        .track(model.track())
        .build();
  }
}
