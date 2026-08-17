package org.cocojojo.mg.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.CurriculumStatusResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.CourseAssignmentMapper;
import org.cocojojo.mg.mapper.CourseMapper;
import org.cocojojo.mg.mapper.GroupMapper;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JTeacher;
import org.cocojojo.mg.util.SecurityUtil;
import org.cocojojo.mg.validator.CourseAssignmentValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseAssignmentService {

  private final CourseAssignmentRepository repository;
  private final CourseService courseService;
  private final TeacherService teacherService;
  private final StudentService studentService;
  private final GroupService groupService;
  private final GroupFlowService groupFlowService;
  private final CourseAssignmentMapper mapper;
  private final CourseMapper courseMapper;
  private final GroupMapper groupMapper;
  private final CourseAssignmentValidator validator;
  private final SecurityUtil securityUtil;

  public List<CourseAssignmentResponse> getByFilter(
      UUID groupId, UUID teacherId, UUID courseId, Integer academicYear) {
    if (securityUtil.isTeacher()) {
      teacherId = securityUtil.getCurrentUserIdOrThrow();
    }
    if (securityUtil.isStudent()) {
      var currentGroup =
          groupFlowService.getCurrentGroup(securityUtil.getCurrentUserIdOrThrow());
      if (currentGroup.isEmpty()) {
        return List.of();
      }
      groupId = currentGroup.get().getId();
    }
    return repository.findFilter(groupId, teacherId, courseId, academicYear).stream()
        .map(mapper::toResponse)
        .toList();
  }

  public CourseAssignmentResponse getById(UUID id) {
    var entity =
        repository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "CourseAssignment with id:" + id + " not found."));
    if (securityUtil.isTeacher()) {
      var currentTeacherId = securityUtil.getCurrentUserIdOrThrow();
      if (entity.getTeachers().stream().noneMatch(t -> t.getId().equals(currentTeacherId))) {
        throw new ForbiddenAccessException("You may only access your own course assignments");
      }
    }
    if (securityUtil.isStudent()) {
      var currentGroup = studentService.getCurrentGroup(securityUtil.getCurrentUserIdOrThrow());
      if (!entity.getGroup().getId().equals(currentGroup.id())) {
        throw new ForbiddenAccessException("This course assignment is not part of your curriculum");
      }
    }
    return mapper.toResponse(entity);
  }

  @Transactional
  public List<CourseAssignmentResponse> upsert(List<CourseAssignmentRequest> requests) {
    validator.validateCreditCeilings(requests);
    return requests.stream().map(this::upsertOne).toList();
  }

  private CourseAssignmentResponse upsertOne(CourseAssignmentRequest request) {
    validator.validateAllAreTeachers(request.teacherIds());
    // Wire the relations with the entities directly; only build the models where the
    // curriculum validation actually needs domain fields.
    var course = courseService.getByIdOrThrow(request.courseId());
    var group = groupService.getByIdOrThrow(request.groupId());
    validator.validateCurriculum(
        courseMapper.toModel(course), groupMapper.toModel(group), request.semester());

    var teachers = request.teacherIds().stream().map(teacherService::getByIdOrThrow).toList();
    var entity =
        request.id() == null
            ? newAssignment(request, course, group, teachers)
            : updateAssignment(request, course, group, teachers);
    return mapper.toResponse(repository.save(entity));
  }

  private JCourseAssignment newAssignment(
      CourseAssignmentRequest request, JCourse course, JGroup group, List<JTeacher> teachers) {
    validator.validateNotDuplicate(
        null, course.getId(), group.getId(), request.academicYear(), request.semester());
    return mapper.toEntity(
        null,
        course,
        group,
        new ArrayList<>(teachers),
        request.academicYear(),
        request.semester(),
        request.credits());
  }

  private JCourseAssignment updateAssignment(
      CourseAssignmentRequest request, JCourse course, JGroup group, List<JTeacher> teachers) {
    var entity =
        repository
            .findById(request.id())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "CourseAssignment with id:" + request.id() + " not found."));
    entity.setCourse(course);
    entity.setGroup(group);
    entity.setTeachers(new ArrayList<>(teachers));
    entity.setAcademicYear(request.academicYear());
    entity.setSemester(request.semester());
    entity.setCredits(request.credits());
    return entity;
  }

  @Transactional
  public void delete(UUID id) {
    var entity =
        repository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "CourseAssignment with id:" + id + " not found."));
    if (!securityUtil.isAdmin()) {
      throw new ForbiddenAccessException("Only an admin can remove a course assignment");
    }
    repository.delete(entity);
  }

  public CurriculumStatusResponse curriculumStatus(
      UUID groupId, int academicYear, Semester semester) {
    var group = groupService.getById(groupId);
    var assignments =
        repository.findByGroupIdAndAcademicYearAndSemester(groupId, academicYear, semester);
    int assignedCredits = assignments.stream().mapToInt(JCourseAssignment::getCredits).sum();
    var target = validator.creditsPerSemester();

    var assignedCourseIds = assignments.stream().map(a -> a.getCourse().getId()).toList();
    var missing =
        courseService.getByStudentLevelOrderByCodeAsc(StudentLevel.of(semester)).stream()
            .filter(c -> c.track() == null || c.track() == group.track())
            .filter(c -> !assignedCourseIds.contains(c.id()))
            .map(courseMapper::toResponse)
            .toList();

    return CurriculumStatusResponse.builder()
        .semester(semester)
        .assignedCredits(assignedCredits)
        .targetCredits(target)
        .complete(assignedCredits == target && missing.isEmpty())
        .missingCourses(missing)
        .assignments(assignments.stream().map(mapper::toResponse).toList())
        .build();
  }
}
