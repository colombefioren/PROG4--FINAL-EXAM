package org.cocojojo.mg.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResultResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.ResultsSummaryResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.YearlyResultResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.model.Fraction;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.CourseAssignmentRepository;
import org.cocojojo.mg.repository.ExamRepository;
import org.cocojojo.mg.repository.GradeRepository;
import org.cocojojo.mg.repository.GroupFlowRepository;
import org.cocojojo.mg.repository.StudentRepository;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.cocojojo.mg.repository.model.JExam;
import org.cocojojo.mg.repository.model.JGrade;
import org.cocojojo.mg.repository.model.JGroupFlow;
import org.cocojojo.mg.repository.model.JStudent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResultService {

  private static final BigDecimal PASS_THRESHOLD = BigDecimal.TEN;

  private final StudentRepository studentRepository;
  private final GroupFlowRepository groupFlowRepository;
  private final CourseAssignmentRepository courseAssignmentRepository;
  private final ExamRepository examRepository;
  private final GradeRepository gradeRepository;

  public YearlyResultResponse computeYearlyResult(UUID studentId, StudentLevel level) {
    var student = findStudent(studentId);
    return computeYearlyResult(student, level, studentGroupIds(studentId));
  }

  public ResultsSummaryResponse computeResultsSummary(UUID studentId) {
    var student = findStudent(studentId);
    var groupIds = studentGroupIds(studentId);

    var levels =
        List.of(StudentLevel.L1, StudentLevel.L2, StudentLevel.L3).stream()
            .map(level -> computeYearlyResult(student, level, groupIds))
            .toList();

    var overallAverage =
        creditWeightedAverage(
            levels.stream()
                .filter(l -> l.overallAverage() != null)
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
      JStudent student, StudentLevel level, List<UUID> groupIds) {
    // The curriculum is the courses actually assigned to the student's groups, not the whole
    // catalog: a promotion that substitutes one course for another (e.g. SYS3 for PROG4) must
    // not penalise its students for a catalog course that was never assigned to them.
    var requiredCourses =
        courseAssignmentRepository.findCurriculumCourses(groupIds, level.semesters());

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
            .filter(c -> Boolean.TRUE.equals(c.passed()))
            .mapToInt(CourseResultResponse::credits)
            .sum();
    int totalCredits = courseResults.stream().mapToInt(CourseResultResponse::credits).sum();
    boolean complete =
        !courseResults.isEmpty()
            && courseResults.stream().allMatch(c -> Boolean.TRUE.equals(c.passed()));

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
    var scheduledExams =
        examRepository.findByCourseAndSemestersAndGroups(course.getId(), semesters, groupIds);
    return buildCourseResult(course, credits, grades, scheduledExams);
  }

  /** Pure computation of one course result, shared by the per-student and the batch paths. */
  private CourseResultResponse buildCourseResult(
      JCourse course, int credits, List<JGrade> grades, List<JExam> scheduledExams) {
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

    // A course is complete only when every currently scheduled exam is graded AND the graded
    // exams cover the full 1.0 coefficient weight. The exam-count check alone would call a
    // course "complete" after a single 1/4 exam is graded, even though 3/4 of its weight has
    // not been scheduled yet.
    boolean allScheduledGraded =
        !scheduledExams.isEmpty() && grades.size() == scheduledExams.size();
    // gradedCoefficientSum can span several course-assignments for the same course (a student who
    // changed groups or retook it). Nothing validates that cross-assignment sum stays <= 1, so if
    // it
    // ever exceeds 1 Fraction::plus throws IllegalArgumentException here, mid-GET of the student's
    // own
    // results. Low likelihood (needs two overlapping assignments), but it would surface as a
    // confusing
    // 400 on a read path.
    var gradedCoefficientSum =
        grades.stream()
            .map(grade -> grade.getExam().getCoefficientFraction())
            .reduce(Fraction::plus)
            .orElseThrow();
    boolean fullWeightGraded = gradedCoefficientSum.equals(new Fraction(1, 1));
    boolean complete = allScheduledGraded && fullWeightGraded;
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

  /**
   * The track of the group the student most recently joined. Used for reporting only: the
   * curriculum itself spans every group the student ever joined (see studentGroupIds).
   */
  public Track currentTrack(UUID studentId) {
    return groupFlowRepository
        .findFirstByStudentIdOrderByCreatedAtDesc(studentId)
        .filter(gf -> gf.getGroupFlowType() == GroupFlowType.JOIN)
        .map(gf -> gf.getGroup().getTrack())
        .orElse(null);
  }

  /**
   * Batch equivalent of {@link #computeResultsSummary(UUID)}: computes every summary with a
   * constant number of queries, whatever the student count. The curriculum, credits and exams are
   * fetched once per distinct set of groups, and only the grades are per-student.
   */
  public Map<UUID, ResultsSummaryResponse> computeResultsSummaries(Collection<JStudent> students) {
    var studentIds = students.stream().map(JStudent::getId).toList();

    var flowsByStudent =
        groupFlowRepository.findByStudentIdIn(studentIds).stream()
            .filter(flow -> flow.getGroupFlowType() == GroupFlowType.JOIN)
            .collect(
                Collectors.groupingBy(
                    flow -> flow.getStudent().getId(),
                    Collectors.mapping(flow -> flow.getGroup().getId(), Collectors.toSet())));
    var gradesByStudent =
        gradeRepository.findByStudentIdIn(studentIds).stream()
            .collect(Collectors.groupingBy(grade -> grade.getStudent().getId()));

    var studentsByGroupSet =
        students.stream()
            .collect(
                Collectors.groupingBy(
                    student -> flowsByStudent.getOrDefault(student.getId(), Set.of())));

    var summaries = new HashMap<UUID, ResultsSummaryResponse>();
    for (var entry : studentsByGroupSet.entrySet()) {
      var groupIds = entry.getKey();
      var groupStudents = entry.getValue();
      var allSemesters = EnumSet.allOf(Semester.class);

      var coursesByLevel = new EnumMap<StudentLevel, List<JCourse>>(StudentLevel.class);
      for (var level : List.of(StudentLevel.L1, StudentLevel.L2, StudentLevel.L3)) {
        coursesByLevel.put(
            level, courseAssignmentRepository.findCurriculumCourses(groupIds, level.semesters()));
      }

      var assignmentsByCourse =
          courseAssignmentRepository.findByGroupIdInAndSemesterIn(groupIds, allSemesters).stream()
              .collect(Collectors.groupingBy(assignment -> assignment.getCourse().getId()));

      var examsByCourse =
          examRepository
              .findByCourseIdsAndSemestersAndGroups(
                  coursesByLevel.values().stream()
                      .flatMap(List::stream)
                      .map(JCourse::getId)
                      .toList(),
                  allSemesters,
                  groupIds)
              .stream()
              .collect(
                  Collectors.groupingBy(exam -> exam.getCourseAssignment().getCourse().getId()));

      for (var student : groupStudents) {
        var grades = gradesByStudent.getOrDefault(student.getId(), List.of());
        var levels =
            List.of(StudentLevel.L1, StudentLevel.L2, StudentLevel.L3).stream()
                .map(
                    level ->
                        computeYearlyResultBatch(
                            student,
                            level,
                            coursesByLevel.get(level),
                            grades,
                            assignmentsByCourse,
                            examsByCourse))
                .toList();

        var overallAverage =
            creditWeightedAverage(
                levels.stream()
                    .filter(l -> l.overallAverage() != null)
                    .map(l -> new WeightedValue(l.overallAverage(), l.totalCredits()))
                    .toList());
        var graduate = levels.stream().allMatch(YearlyResultResponse::complete);

        summaries.put(
            student.getId(),
            ResultsSummaryResponse.builder()
                .studentId(student.getId())
                .studentStd(student.getStd())
                .levels(levels)
                .overallAverage(overallAverage)
                .graduate(graduate)
                .build());
      }
    }
    return summaries;
  }

  /** Batch equivalent of {@link #currentTrack(UUID)}, one query for all students. */
  public Map<UUID, Track> currentTracks(Collection<JStudent> students) {
    return groupFlowRepository
        .findByStudentIdIn(students.stream().map(JStudent::getId).toList())
        .stream()
        .collect(
            Collectors.groupingBy(
                flow -> flow.getStudent().getId(),
                Collectors.collectingAndThen(
                    Collectors.maxBy(Comparator.comparing(JGroupFlow::getCreatedAt)),
                    latest ->
                        latest
                            .filter(flow -> flow.getGroupFlowType() == GroupFlowType.JOIN)
                            .map(flow -> flow.getGroup().getTrack())
                            .orElse(null))));
  }

  private YearlyResultResponse computeYearlyResultBatch(
      JStudent student,
      StudentLevel level,
      List<JCourse> requiredCourses,
      List<JGrade> studentGrades,
      Map<UUID, List<JCourseAssignment>> assignmentsByCourse,
      Map<UUID, List<JExam>> examsByCourse) {
    var courseResults =
        requiredCourses.stream()
            .filter(Objects::nonNull)
            .map(
                course -> {
                  var courseGrades =
                      studentGrades.stream()
                          .filter(
                              grade ->
                                  grade.getExam().getCourseAssignment().getCourse() != null
                                      && grade
                                          .getExam()
                                          .getCourseAssignment()
                                          .getCourse()
                                          .getId()
                                          .equals(course.getId()))
                          .filter(
                              grade ->
                                  level
                                      .semesters()
                                      .contains(
                                          grade.getExam().getCourseAssignment().getSemester()))
                          .toList();
                  var scheduledExams =
                      examsByCourse.getOrDefault(course.getId(), List.of()).stream()
                          .filter(
                              exam ->
                                  level
                                      .semesters()
                                      .contains(exam.getCourseAssignment().getSemester()))
                          .toList();
                  var credits =
                      assignmentsByCourse.getOrDefault(course.getId(), List.of()).stream()
                          .filter(
                              assignment -> level.semesters().contains(assignment.getSemester()))
                          .findFirst()
                          .map(JCourseAssignment::getCredits)
                          .orElse(course.getCredits());
                  return buildCourseResult(course, credits, courseGrades, scheduledExams);
                })
            .toList();

    var overallAverage =
        creditWeightedAverage(
            courseResults.stream()
                .filter(c -> c.average() != null)
                .map(c -> new WeightedValue(c.average(), c.credits()))
                .toList());

    int earnedCredits =
        courseResults.stream()
            .filter(c -> Boolean.TRUE.equals(c.passed()))
            .mapToInt(CourseResultResponse::credits)
            .sum();
    int totalCredits = courseResults.stream().mapToInt(CourseResultResponse::credits).sum();
    boolean complete =
        !courseResults.isEmpty()
            && courseResults.stream().allMatch(c -> Boolean.TRUE.equals(c.passed()));

    return YearlyResultResponse.builder()
        .level(level)
        .courses(courseResults)
        .overallAverage(overallAverage)
        .earnedCredits(earnedCredits)
        .totalCredits(totalCredits)
        .complete(complete)
        .build();
  }

  private JStudent findStudent(UUID studentId) {
    return studentRepository
        .findById(studentId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Student with id:" + studentId + " not found."));
  }

  private BigDecimal toDecimal(Fraction fraction) {
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
