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
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.CourseRepository;
import org.cocojojo.mg.repository.TeacherRepository;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.util.SecurityUtil;
import org.cocojojo.mg.validator.CourseAssignmentValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseAssignmentService {

  private final CourseAssignmentRepository repository;
  private final CourseRepository courseRepository;
  private final TeacherRepository teacherRepository;
  private final GroupService groupService;
  private final CourseAssignmentMapper mapper;
  private final CourseMapper courseMapper;
  private final CourseAssignmentValidator validator;
  private final SecurityUtil securityUtil;

  public List<CourseAssignmentResponse> getByGroup(UUID groupId) {
    return repository.findByGroupId(groupId).stream().map(mapper::toResponse).toList();
  }

  public List<CourseAssignmentResponse> getByTeacher(UUID teacherId) {
    return repository.findByTeachers_Id(teacherId).stream().map(mapper::toResponse).toList();
  }

  public CourseAssignmentResponse getById(UUID id) {
    return mapper.toResponse(find(id));
  }

  public JCourseAssignment find(UUID id) {
    return repository
        .findById(id)
        .orElseThrow(
            () -> new ResourceNotFoundException("CourseAssignment with id:" + id + " not found."));
  }

  @Transactional
  public List<CourseAssignmentResponse> crupdate(List<CourseAssignmentRequest> requests) {
    var saved = requests.stream().map(this::crupdateOne).toList();
    requests.stream()
        .map(r -> new GroupSemesterKey(r.groupId(), r.academicYear(), r.semester()))
        .distinct()
        .forEach(this::validateCeiling);
    return saved;
  }

  private record GroupSemesterKey(UUID groupId, int academicYear, Semester semester) {}

  private void validateCeiling(GroupSemesterKey key) {
    var assignments =
        repository.findByGroupIdAndAcademicYearAndSemester(
            key.groupId(), key.academicYear(), key.semester());
    validator.validateCreditCeiling(assignments);
  }

  private CourseAssignmentResponse crupdateOne(CourseAssignmentRequest request) {
    var course =
        courseRepository
            .findById(request.courseId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Course with id:" + request.courseId() + " not found."));
    var group = groupService.find(request.groupId());
    validator.validateTrackCompatibility(course, group);

    if (course.getStudentLevel() != StudentLevel.of(request.semester())) {
      throw new InvalidCurriculumException(
          "Course "
              + course.getCode()
              + " is a "
              + course.getStudentLevel()
              + " course, not compatible with "
              + request.semester());
    }

    var teachers =
        request.teacherIds().stream()
            .map(teacherRepository::findById)
            .map(o -> o.orElseThrow(() -> new ResourceNotFoundException("Teacher not found")))
            .toList();

    JCourseAssignment entity;
    if (request.id() != null) {
      entity = find(request.id());
      entity.setCourse(course);
      entity.setGroup(group);
      entity.setTeachers(teachers);
      entity.setAcademicYear(request.academicYear());
      entity.setSemester(request.semester());
      entity.setCredits(request.credits());
    } else {
      validator.validateNotDuplicate(
          request.id(), course.getId(), group.getId(), request.academicYear(), request.semester());
      entity =
          mapper.toEntity(
              null,
              course,
              group,
              teachers,
              request.academicYear(),
              request.semester(),
              request.credits());
    }
    return mapper.toResponse(repository.save(entity));
  }

  @Transactional
  public void delete(UUID id) {
    var entity = find(id);
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
    var group = groupService.find(groupId);
    var assignments =
        repository.findByGroupIdAndAcademicYearAndSemester(groupId, academicYear, semester);
    int assignedCredits = assignments.stream().mapToInt(JCourseAssignment::getCredits).sum();
    var target = validator.creditsPerSemester();

    var assignedCourseIds = assignments.stream().map(a -> a.getCourse().getId()).toList();
    var missing =
        courseRepository.findByStudentLevelOrderByCodeAsc(StudentLevel.of(semester)).stream()
            .filter(c -> c.getTrack() == null || c.getTrack() == group.getTrack())
            .filter(c -> !assignedCourseIds.contains(c.getId()))
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
