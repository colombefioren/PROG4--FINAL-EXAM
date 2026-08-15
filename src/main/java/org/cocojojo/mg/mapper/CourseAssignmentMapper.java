package org.cocojojo.mg.mapper;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentResponse;
import org.cocojojo.mg.model.CourseAssignment;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JTeacher;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CourseAssignmentMapper {

  private final CourseMapper courseMapper;
  private final GroupMapper groupMapper;
  private final TeacherMapper teacherMapper;

  public JCourseAssignment toEntity(
      UUID id,
      JCourse course,
      JGroup group,
      List<JTeacher> teachers,
      int academicYear,
      Semester semester,
      int credits) {
    return JCourseAssignment.builder()
        .id(id)
        .course(course)
        .group(group)
        .teachers(teachers)
        .academicYear(academicYear)
        .semester(semester)
        .credits(credits)
        .build();
  }

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

  public CourseAssignmentResponse toResponse(JCourseAssignment entity) {
    return toResponse(toModel(entity));
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
