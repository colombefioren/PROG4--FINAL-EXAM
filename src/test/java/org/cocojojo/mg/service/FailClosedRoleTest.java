package org.cocojojo.mg.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.exception.ForbiddenAccessException;
import org.cocojojo.mg.mapper.CourseAssignmentMapper;
import org.cocojojo.mg.mapper.ExamMapper;
import org.cocojojo.mg.mapper.GradeHistoryMapper;
import org.cocojojo.mg.mapper.GradeMapper;
import org.cocojojo.mg.model.CourseAssignment;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.ExamRepository;
import org.cocojojo.mg.repository.GradeHistoryRepository;
import org.cocojojo.mg.repository.GradeRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.UserRepository;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JGrade;
import org.cocojojo.mg.util.SecurityUtil;
import org.cocojojo.mg.validator.ExamValidator;
import org.cocojojo.mg.validator.GradeValidator;
import org.junit.jupiter.api.Test;

/** Read paths must fail closed when no known role matches, instead of silently granting access. */
class FailClosedRoleTest {

  @Test
  void gradeReadFailsClosedWhenNoRoleMatches() {
    var gradeRepository = mock(GradeRepository.class);
    when(gradeRepository.findWithDetailsById(any(UUID.class)))
        .thenReturn(Optional.of(mock(JGrade.class)));
    var service =
        new GradeService(
            gradeRepository,
            mock(GradeHistoryRepository.class),
            mock(ExamRepository.class),
            mock(StudentRepository.class),
            mock(UserRepository.class),
            mock(GradeMapper.class),
            mock(GradeHistoryMapper.class),
            mock(CourseAssignmentMapper.class),
            mock(GradeValidator.class),
            mock(SecurityUtil.class));

    assertThrows(ForbiddenAccessException.class, () -> service.getById(UUID.randomUUID()));
  }

  @Test
  void examReadFailsClosedWhenNoRoleMatches() {
    var courseAssignmentRepository = mock(CourseAssignmentRepository.class);
    when(courseAssignmentRepository.findById(any(UUID.class)))
        .thenReturn(Optional.of(mock(JCourseAssignment.class)));
    var courseAssignmentMapper = mock(CourseAssignmentMapper.class);
    when(courseAssignmentMapper.toModel(any())).thenReturn(mock(CourseAssignment.class));
    var service =
        new ExamService(
            mock(ExamRepository.class),
            courseAssignmentRepository,
            mock(ExamMapper.class),
            courseAssignmentMapper,
            mock(ExamValidator.class),
            mock(SecurityUtil.class));

    assertThrows(
        ForbiddenAccessException.class,
        () -> service.getByCourseAssignmentId(UUID.randomUUID(), null, null));
  }
}
