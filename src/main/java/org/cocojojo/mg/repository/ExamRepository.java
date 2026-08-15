package org.cocojojo.mg.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.repository.model.JExam;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends JpaRepository<JExam, UUID> {

  @EntityGraph(attributePaths = {"courseAssignment", "courseAssignment.teachers"})
  List<JExam> findByCourseAssignmentId(UUID courseAssignmentId);

  @EntityGraph(attributePaths = {"courseAssignment", "courseAssignment.teachers"})
  @Query(
      """
      select e from JExam e
      where e.courseAssignment.id = :courseAssignmentId
        and e.examDatetime >= coalesce(:from, e.examDatetime)
        and e.examDatetime <= coalesce(:to, e.examDatetime)
      """)
  List<JExam> findByCourseAssignmentIdAndDateRange(
      @Param("courseAssignmentId") UUID courseAssignmentId,
      @Param("from") Instant from,
      @Param("to") Instant to);
}
