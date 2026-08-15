package org.cocojojo.mg.mapper;

import lombok.AllArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentResponse;
import org.cocojojo.mg.model.CourseAssignment;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CourseAssignmentMapper {

  private final CourseMapper courseMapper;
  private final GroupMapper groupMapper;
  private final TeacherMapper teacherMapper;

  public CourseAssignment toModel(JCourseAssignment entity) {
    return CourseAssignment.builder()
        .id(entity.getId())
        .course(courseMapper.toModel(entity.getCourse()))
        .group(groupMapper.toModel(entity.getGroup()))
        .teachers(entity.getTeachers().stream().map(teacherMapper::toModel).toList())
        .academicYear(entity.getAcademicYear())
        .semester(entity.getSemester())
        .credits(entity.getCredits())
        .build();
  }

  public CourseAssignmentResponse toResponse(CourseAssignment model) {
    return CourseAssignmentResponse.builder()
        .id(model.id())
        .courseId(model.course().id())
        .courseCode(model.course().code())
        .courseName(model.course().name())
        .groupId(model.group().id())
        .groupRef(model.group().ref())
        .teachers(model.teachers().stream().map(teacherMapper::toResponse).toList())
        .academicYear(model.academicYear())
        .semester(model.semester())
        .credits(model.credits())
        .build();
  }
}
