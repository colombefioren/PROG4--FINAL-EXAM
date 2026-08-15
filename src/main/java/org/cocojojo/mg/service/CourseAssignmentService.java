package org.cocojojo.mg.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.CurriculumStatusResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.endpoint.rest.controller.exception.InvalidCurriculumException;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.mapper.CourseAssignmentMapper;
import org.cocojojo.mg.mapper.CourseMapper;
import org.cocojojo.mg.mapper.GroupMapper;
import org.cocojojo.mg.mapper.TeacherMapper;
import org.cocojojo.mg.model.Course;
import org.cocojojo.mg.model.Group;
import org.cocojojo.mg.model.Teacher;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.model.JCourseAssignment;
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
  private final CourseAssignmentMapper mapper;
  private final CourseMapper courseMapper;
  private final GroupMapper groupMapper;
  private final TeacherMapper teacherMapper;
  private final CourseAssignmentValidator validator;
  private final SecurityUtil securityUtil;

  public List<CourseAssignmentResponse> getByFilter(
      UUID groupId, UUID teacherId, UUID courseId, Integer academicYear) {
    if (securityUtil.isTeacher()) {
      teacherId = securityUtil.getCurrentUserIdOrThrow();
    }
    if (securityUtil.isStudent()) {
      groupId = studentService.getCurrentGroup(securityUtil.getCurrentUserIdOrThrow()).id();
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
    var saved = requests.stream().map(this::upsertOne).toList();
    requests.forEach(r -> validateCeiling(r.groupId(), r.academicYear(), r.semester()));
    return saved;
  }

  private void validateCeiling(UUID groupId, int academicYear, Semester semester) {
    var assignments =
        repository.findByGroupIdAndAcademicYearAndSemester(groupId, academicYear, semester);
    validator.validateCreditCeiling(assignments);
  }

  private CourseAssignmentResponse upsertOne(CourseAssignmentRequest request) {
    var course = courseService.getById(request.courseId());
    var group = groupService.getById(request.groupId());
    validateCurriculum(request, course, group);

    var teachers = request.teacherIds().stream().map(teacherService::getById).toList();
    var entity =
        request.id() == null
            ? newAssignment(request, course, group, teachers)
            : updateAssignment(request, course, group, teachers);
    return mapper.toResponse(repository.save(entity));
  }

  private void validateCurriculum(CourseAssignmentRequest request, Course course, Group group) {
    validator.validateTrackCompatibility(course, group);
    if (course.studentLevel() != StudentLevel.of(request.semester())) {
      throw new InvalidCurriculumException(
          "Course "
              + course.code()
              + " is a "
              + course.studentLevel()
              + " course, not compatible with "
              + request.semester());
    }
  }

  private JCourseAssignment newAssignment(
      CourseAssignmentRequest request, Course course, Group group, List<Teacher> teachers) {
    validator.validateNotDuplicate(
        null, course.id(), group.id(), request.academicYear(), request.semester());
    return mapper.toEntity(
        null,
        courseMapper.toEntity(course),
        groupMapper.toEntity(group),
        teachers.stream().map(teacherMapper::toEntity).toList(),
        request.academicYear(),
        request.semester(),
        request.credits());
  }

  private JCourseAssignment updateAssignment(
      CourseAssignmentRequest request, Course course, Group group, List<Teacher> teachers) {
    var entity =
        repository
            .findById(request.id())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "CourseAssignment with id:" + request.id() + " not found."));
    entity.setCourse(courseMapper.toEntity(course));
    entity.setGroup(groupMapper.toEntity(group));
    entity.setTeachers(teachers.stream().map(teacherMapper::toEntity).toList());
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

  /**
   * Non-destructive report: current credit total vs. the 30-credit target, and which catalog UEs
   * are missing.
   */
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
