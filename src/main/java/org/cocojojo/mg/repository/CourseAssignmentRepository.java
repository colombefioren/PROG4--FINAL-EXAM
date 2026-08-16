package org.cocojojo.mg.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.repository.model.JCourse;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseAssignmentRepository extends JpaRepository<JCourseAssignment, UUID> {

  boolean existsByCourseIdAndGroupIdAndAcademicYearAndSemester(
      UUID courseId, UUID groupId, int academicYear, Semester semester);

  @EntityGraph(attributePaths = {"course"})
  List<JCourseAssignment> findByGroupIdIn(List<UUID> groupIds);

  /** The distinct courses actually assigned to the given groups within the given semesters. */
  @Query(
      """
      select distinct a.course from JCourseAssignment a
      where a.group.id in :groupIds
        and a.semester in :semesters
      """)
  List<JCourse> findCurriculumCourses(
      @Param("groupIds") Collection<UUID> groupIds,
      @Param("semesters") Collection<Semester> semesters);

  @Query(
      """
      select a from JCourseAssignment a
      where a.course.id = :courseId
        and a.semester in :semesters
        and a.group.id in :groupIds
      """)
  @EntityGraph(attributePaths = {"course"})
  List<JCourseAssignment> findByCourseIdAndSemesterInAndGroupIdIn(
      @Param("courseId") UUID courseId,
      @Param("semesters") Collection<Semester> semesters,
      @Param("groupIds") Collection<UUID> groupIds);
}
