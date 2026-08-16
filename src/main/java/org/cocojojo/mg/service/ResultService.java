package org.cocojojo.mg.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResultResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.ResultsSummaryResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.YearlyResultResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.model.enums.ResultStatus;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.CourseRepository;
import org.cocojojo.mg.repository.GradeRepository;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JGroup;
import org.cocojojo.mg.repository.model.JGroupFlow;
import org.cocojojo.mg.repository.model.JStudent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResultService {

  private static final BigDecimal PASSING_GRADE = new BigDecimal("10");
  private static final List<StudentLevel> UPPER_LEVELS = List.of(StudentLevel.L2, StudentLevel.L3);

  private final StudentRepository studentRepository;
  private final GroupFlowRepository groupFlowRepository;
  private final CourseRepository courseRepository;
  private final GradeRepository gradeRepository;

  /** Full 3-year summary: L1 common courses plus L2/L3 courses of the student's track. */
  public ResultsSummaryResponse computeResultsSummary(UUID studentId) {
    var student = getStudent(studentId);
    var track = trackOf(student);
    var curriculum = curriculumOf(student, track);
    var averagesByCourse = averagesByCourse(studentId);

    var levels = new ArrayList<YearlyResultResponse>();
    for (var level : StudentLevel.values()) {
      var levelCourses = new ArrayList<CourseResultResponse>();
      for (var course : curriculum) {
        if (course.getStudentLevel() != level) {
          continue;
        }
        var average = averagesByCourse.get(course.getId());
        var passed = average != null && average.compareTo(PASSING_GRADE) >= 0;
        levelCourses.add(
            CourseResultResponse.builder()
                .courseId(course.getId())
                .courseCode(course.getCode())
                .courseName(course.getName())
                .credits(course.getCredits())
                .track(course.getTrack())
                .average(average)
                .resultStatus(passed ? ResultStatus.COMPLETED : ResultStatus.PROVISIONAL)
                .passed(passed)
                .build());
      }
      if (levelCourses.isEmpty()) {
        continue;
      }
      var allPassed = levelCourses.stream().allMatch(CourseResultResponse::passed);
      var earnedCredits =
          levelCourses.stream()
              .filter(CourseResultResponse::passed)
              .mapToInt(CourseResultResponse::credits)
              .sum();
      var totalCredits = levelCourses.stream().mapToInt(CourseResultResponse::credits).sum();
      levels.add(
          YearlyResultResponse.builder()
              .level(level)
              .courses(levelCourses)
              .overallAverage(overallAverage(levelCourses))
              .earnedCredits(earnedCredits)
              .totalCredits(totalCredits)
              .resultStatus(allPassed ? ResultStatus.COMPLETED : ResultStatus.PROVISIONAL)
              .build());
    }

    var allCourses = levels.stream().flatMap(l -> l.courses().stream()).toList();
    var graduate =
        !allCourses.isEmpty() && allCourses.stream().allMatch(c -> Boolean.TRUE.equals(c.passed()));
    return ResultsSummaryResponse.builder()
        .studentId(student.getId())
        .studentStd(student.getStd())
        .levels(levels)
        .overallAverage(overallAverage(allCourses))
        .graduate(graduate)
        .build();
  }

  private JStudent getStudent(UUID studentId) {
    return studentRepository
        .findById(studentId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Student with id:" + studentId + " not found."));
  }

  /** A student's track is the track of the group they most recently joined. */
  private Track trackOf(JStudent student) {
    return groupFlowRepository.findByStudentId(student.getId()).stream()
        .max(Comparator.comparing(JGroupFlow::getCreatedAt))
        .map(JGroupFlow::getGroup)
        .map(JGroup::getTrack)
        .orElse(null);
  }

  /** Curriculum: L1 common courses plus L2/L3 courses of the student's track. */
  private List<JCourse> curriculumOf(JStudent student, Track track) {
    var l1 = courseRepository.findByStudentLevel(StudentLevel.L1);
    if (track == null) {
      return l1;
    }
    var upper = courseRepository.findByStudentLevelInAndTrack(UPPER_LEVELS, track);
    var result = new ArrayList<JCourse>(l1);
    result.addAll(upper);
    return result;
  }

  /** Map course id -> average of the student's grades on exams of that course. */
  private Map<UUID, BigDecimal> averagesByCourse(UUID studentId) {
    return gradeRepository.findByStudentId(studentId).stream()
        .collect(
            Collectors.groupingBy(
                g -> g.getExam().getCourseAssignment().getCourse().getId(),
                Collectors.averagingDouble(g -> g.getValue().doubleValue())))
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e -> BigDecimal.valueOf(e.getValue()).setScale(2, RoundingMode.HALF_UP)));
  }

  private BigDecimal overallAverage(List<CourseResultResponse> courses) {
    var withGrades = courses.stream().filter(c -> c.average() != null).toList();
    if (withGrades.isEmpty()) {
      return null;
    }
    var sum =
        withGrades.stream()
            .map(CourseResultResponse::average)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return sum.divide(BigDecimal.valueOf(withGrades.size()), 2, RoundingMode.HALF_UP);
  }
}
