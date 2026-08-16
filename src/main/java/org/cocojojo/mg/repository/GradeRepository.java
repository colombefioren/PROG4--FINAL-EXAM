package org.cocojojo.mg.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.repository.model.JGrade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRepository extends JpaRepository<JGrade, UUID> {

  @EntityGraph(
      attributePaths = {
        "exam",
        "exam.courseAssignment",
        "exam.courseAssignment.teachers",
        "student"
      })
  List<JGrade> findByExamId(UUID examId);

  @EntityGraph(
      attributePaths = {
        "exam",
        "exam.courseAssignment",
        "exam.courseAssignment.teachers",
        "student"
      })
  List<JGrade> findByStudentId(UUID studentId);

  @Query(
      """
      select g from JGrade g
      where g.student.id = :studentId
        and g.exam.courseAssignment.course.id = :courseId
        and g.exam.courseAssignment.semester in :semesters
      """)
  @EntityGraph(
      attributePaths = {
        "exam",
        "exam.courseAssignment",
        "exam.courseAssignment.course",
        "exam.courseAssignment.teachers",
        "student"
      })
  List<JGrade> findByStudentAndCourseAndSemesters(
      @Param("studentId") UUID studentId,
      @Param("courseId") UUID courseId,
      @Param("semesters") Collection<Semester> semesters);

  @EntityGraph(
      attributePaths = {
        "exam",
        "exam.courseAssignment",
        "exam.courseAssignment.course",
        "exam.courseAssignment.teachers",
        "student"
      })
  Optional<JGrade> findWithDetailsById(UUID id);

  @EntityGraph(
      attributePaths = {
        "exam",
        "exam.courseAssignment",
        "exam.courseAssignment.teachers",
        "student"
      })
  Optional<JGrade> findByExamIdAndStudentId(UUID examId, UUID studentId);
}
