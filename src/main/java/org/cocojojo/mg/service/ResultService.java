package org.cocojojo.mg.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
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
import org.cocojojo.mg.model.enums.ResultStatus;
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
import org.cocojojo.mg.util.SemesterCalculator;
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
    return computeYearlyResult(student, level, studentGroupIds(studentId), currentTrack(student));
  }

  public ResultsSummaryResponse computeResultsSummary(UUID studentId) {
    var student = findStudent(studentId);
    var groupIds = studentGroupIds(studentId);
    var track = currentTrack(student);

    var levels =
        List.of(StudentLevel.L1, StudentLevel.L2, StudentLevel.L3).stream()
            .map(level -> computeYearlyResult(student, level, groupIds, track))
            .toList();

    var overallAverage =
        creditWeightedAverage(
            levels.stream()
                .filter(l -> l.overallAverage() != null)
                .map(l -> new WeightedValue(l.overallAverage(), l.totalCredits()))
                .toList());
    var graduate = levels.stream().allMatch(y -> y.status() == ResultStatus.COMPLETED);

    return ResultsSummaryResponse.builder()
        .studentId(student.getId())
        .studentStd(student.getStd())
        .levels(levels)
        .overallAverage(overallAverage)
        .graduate(graduate)
        .build();
  }

  private YearlyResultResponse computeYearlyResult(
      JStudent student, StudentLevel level, List<UUID> groupIds, Track currentTrack) {
    var requiredCourses =
        courseAssignmentRepository.findCurriculumCourses(groupIds, level.semesters()).stream()
            .filter(course -> isTrackCompatible(course, currentTrack))
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
        .status(complete ? ResultStatus.COMPLETED : ResultStatus.PROVISIONAL)
        .build();
  }

  private CourseResultResponse computeCourseResult(
      JStudent student, JCourse course, StudentLevel level, List<UUID> groupIds) {
    var semesters = level.semesters();
    var latestAssignment =
        resolveLatestAssignment(
            courseAssignmentRepository.findByCourseIdAndSemesterInAndGroupIdIn(
                course.getId(), semesters, groupIds));
    if (latestAssignment.isEmpty()) {
      return buildCourseResult(course, course.getCredits(), List.of(), List.of());
    }
    var assignment = latestAssignment.get();
    var grades =
        gradeRepository
            .findByStudentAndCourseAndSemesters(student.getId(), course.getId(), semesters)
            .stream()
            .filter(g -> g.getExam().getCourseAssignment().getId().equals(assignment.getId()))
            .toList();
    var scheduledExams =
        examRepository
            .findByCourseAndSemestersAndGroups(course.getId(), semesters, groupIds)
            .stream()
            .filter(e -> e.getCourseAssignment().getId().equals(assignment.getId()))
            .toList();
    return buildCourseResult(course, assignment.getCredits(), grades, scheduledExams);
  }

  private java.util.Optional<JCourseAssignment> resolveLatestAssignment(
      List<JCourseAssignment> assignments) {
    return assignments.stream()
        .max(
            Comparator.comparing(JCourseAssignment::getAcademicYear)
                .thenComparing(JCourseAssignment::getId));
  }

  private boolean isTrackCompatible(JCourse course, Track currentTrack) {
    return currentTrack == null || course.getTrack() == null || course.getTrack() == currentTrack;
  }

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
          .passed(null)
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

    boolean allScheduledGraded =
        !scheduledExams.isEmpty() && grades.size() == scheduledExams.size();
    BigInteger sumNumerator = BigInteger.ZERO;
    BigInteger sumDenominator = BigInteger.ONE;
    for (var grade : grades) {
      var f = grade.getExam().getCoefficientFraction();
      sumNumerator =
          sumNumerator
              .multiply(BigInteger.valueOf(f.denominator()))
              .add(BigInteger.valueOf(f.numerator()).multiply(sumDenominator));
      sumDenominator = sumDenominator.multiply(BigInteger.valueOf(f.denominator()));
    }
    boolean fullWeightGraded = sumNumerator.equals(sumDenominator);
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

  private List<UUID> studentGroupIds(UUID studentId) {
    return groupFlowRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
        .filter(gf -> gf.getGroupFlowType() == GroupFlowType.JOIN)
        .map(gf -> gf.getGroup().getId())
        .distinct()
        .toList();
  }

  public Track currentTrack(UUID studentId) {
    return currentTrack(findStudent(studentId));
  }

  public Track currentTrack(JStudent student) {
    return groupFlowRepository
        .findFirstByStudentIdOrderByCreatedAtDesc(student.getId())
        .filter(gf -> gf.getGroupFlowType() == GroupFlowType.JOIN)
        .map(gf -> effectiveTrack(student, gf.getGroup().getTrack()))
        .orElse(null);
  }

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
    var tracksByStudent = currentTracks(students);

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
        var currentTrack = tracksByStudent.getOrDefault(student.getId(), null);
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
                            examsByCourse,
                            currentTrack))
                .toList();

        var overallAverage =
            creditWeightedAverage(
                levels.stream()
                    .filter(l -> l.overallAverage() != null)
                    .map(l -> new WeightedValue(l.overallAverage(), l.totalCredits()))
                    .toList());
        var graduate = levels.stream().allMatch(y -> y.status() == ResultStatus.COMPLETED);

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

  public Map<UUID, Track> currentTracks(Collection<JStudent> students) {
    var studentsById = students.stream().collect(Collectors.toMap(JStudent::getId, s -> s));
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
                            .map(
                                flow ->
                                    effectiveTrack(
                                        studentsById.get(flow.getStudent().getId()),
                                        flow.getGroup().getTrack()))
                            .orElse(null))));
  }

  private Track effectiveTrack(JStudent student, Track groupTrack) {
    if (groupTrack != null) {
      return groupTrack;
    }
    if (student.getPromotion() == null) {
      return null;
    }
    var semester =
        SemesterCalculator.semesterFor(student.getPromotion().getEntryYear(), LocalDate.now());
    if (semester.compareTo(Semester.S4) >= 0) {
      return Track.EL;
    }
    return null;
  }

  private YearlyResultResponse computeYearlyResultBatch(
      JStudent student,
      StudentLevel level,
      List<JCourse> requiredCourses,
      List<JGrade> studentGrades,
      Map<UUID, List<JCourseAssignment>> assignmentsByCourse,
      Map<UUID, List<JExam>> examsByCourse,
      Track currentTrack) {
    var courseResults =
        requiredCourses.stream()
            .filter(Objects::nonNull)
            .filter(course -> isTrackCompatible(course, currentTrack))
            .map(
                course -> {
                  var levelAssignments =
                      assignmentsByCourse.getOrDefault(course.getId(), List.of()).stream()
                          .filter(
                              assignment -> level.semesters().contains(assignment.getSemester()))
                          .toList();
                  var latestAssignment = resolveLatestAssignment(levelAssignments);
                  if (latestAssignment.isEmpty()) {
                    return buildCourseResult(course, course.getCredits(), List.of(), List.of());
                  }
                  var assignment = latestAssignment.get();
                  var courseGrades =
                      studentGrades.stream()
                          .filter(
                              grade ->
                                  grade
                                      .getExam()
                                      .getCourseAssignment()
                                      .getId()
                                      .equals(assignment.getId()))
                          .toList();
                  var scheduledExams =
                      examsByCourse.getOrDefault(course.getId(), List.of()).stream()
                          .filter(
                              exam -> exam.getCourseAssignment().getId().equals(assignment.getId()))
                          .toList();
                  return buildCourseResult(
                      course, assignment.getCredits(), courseGrades, scheduledExams);
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
        .status(complete ? ResultStatus.COMPLETED : ResultStatus.PROVISIONAL)
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
