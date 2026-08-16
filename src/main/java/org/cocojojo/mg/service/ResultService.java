package org.cocojojo.mg.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResultResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.ResultsSummaryResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.YearlyResultResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.CourseRepository;
import org.cocojojo.mg.repository.ExamRepository;
import org.cocojojo.mg.repository.GradeRepository;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JStudent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResultService {

  private static final BigDecimal PASS_THRESHOLD = BigDecimal.TEN;

  private final StudentRepository studentRepository;
  private final GroupFlowRepository groupFlowRepository;
  private final CourseRepository courseRepository;
  private final CourseAssignmentRepository courseAssignmentRepository;
  private final ExamRepository examRepository;
  private final GradeRepository gradeRepository;

  public ResultsSummaryResponse computeResultsSummary(UUID studentId) {
    var student = findStudent(studentId);
    var groupIds = studentGroupIds(studentId);
    var track = currentTrack(studentId);

    var levels =
        List.of(StudentLevel.L1, StudentLevel.L2, StudentLevel.L3).stream()
            .map(level -> computeYearlyResult(student, level, groupIds, track))
            .toList();

    var overallAverage =
        creditWeightedAverage(
            levels.stream()
                .map(l -> new WeightedValue(l.overallAverage(), l.totalCredits()))
                .toList());
    var graduate = levels.stream().allMatch(YearlyResultResponse::complete);

    return ResultsSummaryResponse.builder()
        .studentId(student.getId())
        .studentStd(student.getStd())
        .levels(levels)
        .overallAverage(overallAverage)
        .graduate(graduate)
        .build();
  }

  private YearlyResultResponse computeYearlyResult(
      JStudent student, StudentLevel level, List<UUID> groupIds, Track track) {
    var requiredCourses =
        courseRepository.findByStudentLevelOrderByCodeAsc(level).stream()
            .filter(c -> c.getTrack() == null || c.getTrack() == track)
            .toList();

    var courseResults =
        requiredCourses.stream()
            .map(course -> computeCourseResult(student, course, level, groupIds))
            .toList();

    var overallAverage =
        creditWeightedAverage(
            courseResults.stream()
                .filter(c -> c.average() != null)
                .map(c -> new WeightedValue(c.average(), c.credits()))
                .toList());

    int earnedCredits =
        courseResults.stream()
            .filter(CourseResultResponse::passed)
            .mapToInt(CourseResultResponse::credits)
            .sum();
    int totalCredits = courseResults.stream().mapToInt(CourseResultResponse::credits).sum();
    boolean complete =
        !courseResults.isEmpty() && courseResults.stream().allMatch(CourseResultResponse::passed);

    return YearlyResultResponse.builder()
        .level(level)
        .courses(courseResults)
        .overallAverage(overallAverage)
        .earnedCredits(earnedCredits)
        .totalCredits(totalCredits)
        .complete(complete)
        .build();
  }

  private CourseResultResponse computeCourseResult(
      JStudent student, JCourse course, StudentLevel level, List<UUID> groupIds) {
    var semesters = level.semesters();
    var grades =
        gradeRepository.findByStudentAndCourseAndSemesters(
            student.getId(), course.getId(), semesters);
    var relevantAssignments =
        courseAssignmentRepository.findByCourseIdAndSemesterInAndGroupIdIn(
            course.getId(), semesters, groupIds);
    int credits =
        relevantAssignments.stream()
            .findFirst()
            .map(a -> a.getCredits())
            .orElse(course.getCredits());

    if (grades.isEmpty()) {
      return CourseResultResponse.builder()
          .courseId(course.getId())
          .courseCode(course.getCode())
          .courseName(course.getName())
          .credits(credits)
          .track(course.getTrack())
          .average(null)
          .graded(false)
          .complete(false)
          .passed(false)
          .build();
    }

    BigDecimal weightedSum = BigDecimal.ZERO;
    BigDecimal totalCoefficient = BigDecimal.ZERO;
    for (var grade : grades) {
      var coefficient = grade.getExam().getCoefficientFraction();
      var coefficientDecimal = toDecimal(coefficient);
      weightedSum = weightedSum.add(grade.getValue().multiply(coefficientDecimal));
      totalCoefficient = totalCoefficient.add(coefficientDecimal);
    }
    var average =
        totalCoefficient.compareTo(BigDecimal.ZERO) == 0
            ? null
            : weightedSum.divide(totalCoefficient, 2, RoundingMode.HALF_UP);

    int expectedExamCount =
        examRepository
            .findByCourseAndSemestersAndGroups(course.getId(), semesters, groupIds)
            .size();
    boolean complete = expectedExamCount > 0 && grades.size() == expectedExamCount;
    boolean passed = complete && average != null && average.compareTo(PASS_THRESHOLD) >= 0;

    return CourseResultResponse.builder()
        .courseId(course.getId())
        .courseCode(course.getCode())
        .courseName(course.getName())
        .credits(credits)
        .track(course.getTrack())
        .average(average)
        .graded(true)
        .complete(complete)
        .passed(passed)
        .build();
  }

  /** All groups the student ever joined, so grades from a previous group still count. */
  private List<UUID> studentGroupIds(UUID studentId) {
    return groupFlowRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
        .filter(gf -> gf.getGroupFlowType() == GroupFlowType.JOIN)
        .map(gf -> gf.getGroup().getId())
        .distinct()
        .toList();
  }

  /** A student's track is the track of the group they most recently joined. */
  public Track currentTrack(UUID studentId) {
    return groupFlowRepository
        .findFirstByStudentIdOrderByCreatedAtDesc(studentId)
        .filter(gf -> gf.getGroupFlowType() == GroupFlowType.JOIN)
        .map(gf -> gf.getGroup().getTrack())
        .orElse(null);
  }

  private JStudent findStudent(UUID studentId) {
    return studentRepository
        .findById(studentId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Student with id:" + studentId + " not found."));
  }

  private BigDecimal toDecimal(org.cocojojo.mg.model.Fraction fraction) {
    return BigDecimal.valueOf(fraction.numerator())
        .divide(BigDecimal.valueOf(fraction.denominator()), 6, RoundingMode.HALF_UP);
  }

  private record WeightedValue(BigDecimal value, int weight) {}

  private BigDecimal creditWeightedAverage(List<WeightedValue> values) {
    var totalWeight = values.stream().mapToInt(WeightedValue::weight).sum();
    if (totalWeight == 0) {
      return null;
    }
    var weightedSum =
        values.stream()
            .map(v -> v.value().multiply(BigDecimal.valueOf(v.weight())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return weightedSum.divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
  }
}
