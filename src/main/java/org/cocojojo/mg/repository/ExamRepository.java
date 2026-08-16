package org.cocojojo.mg.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.repository.model.JExam;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends JpaRepository<JExam, UUID> {

  @EntityGraph(attributePaths = {"courseAssignment", "courseAssignment.teachers"})
  Optional<JExam> findWithDetailsById(UUID id);

  @EntityGraph(attributePaths = {"courseAssignment", "courseAssignment.teachers"})
  List<JExam> findByCourseAssignmentId(UUID courseAssignmentId);

  @Query(
      """
      select e from JExam e
      where e.courseAssignment.course.id = :courseId
        and e.courseAssignment.semester in :semesters
        and e.courseAssignment.group.id in :groupIds
      """)
  @EntityGraph(attributePaths = {"courseAssignment", "courseAssignment.teachers"})
  List<JExam> findByCourseAndSemestersAndGroups(
      @Param("courseId") UUID courseId,
      @Param("semesters") Collection<Semester> semesters,
      @Param("groupIds") Collection<UUID> groupIds);
}
