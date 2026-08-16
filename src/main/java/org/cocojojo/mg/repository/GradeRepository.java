package org.cocojojo.mg.repository;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.repository.model.JGrade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
