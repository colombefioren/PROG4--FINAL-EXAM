package org.cocojojo.mg.repository;

import java.util.Collection;
import java.util.List;
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
        "exam.courseAssignment.course",
        "exam.courseAssignment.teachers"
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
        "exam.courseAssignment.teachers"
      })
  List<JGrade> findByStudentAndCourseAndSemesters(
      @Param("studentId") UUID studentId,
      @Param("courseId") UUID courseId,
      @Param("semesters") Collection<Semester> semesters);
}
