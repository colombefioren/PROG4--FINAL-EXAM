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

  @Query(
      """
      select ca from JCourseAssignment ca
      where (cast(:groupId as uuid) is null or ca.group.id = :groupId)
        and (cast(:teacherId as uuid) is null
          or exists (select t from ca.teachers t where t.id = :teacherId))
        and (cast(:courseId as uuid) is null or ca.course.id = :courseId)
        and (cast(:academicYear as integer) is null or ca.academicYear = :academicYear)
      """)
  List<JCourseAssignment> findFilter(
      @Param("groupId") UUID groupId,
      @Param("teacherId") UUID teacherId,
      @Param("courseId") UUID courseId,
      @Param("academicYear") Integer academicYear);

  List<JCourseAssignment> findByGroupIdAndAcademicYearAndSemester(
      UUID groupId, int academicYear, Semester semester);

  @Query(
      """
      select a from JCourseAssignment a
      where a.group.id in :groupIds
        and a.semester in :semesters
        and a.course.isDeleted = false
      """)
  @EntityGraph(attributePaths = {"course"})
  List<JCourseAssignment> findByGroupIdInAndSemesterIn(
      @Param("groupIds") Collection<UUID> groupIds,
      @Param("semesters") Collection<Semester> semesters);

  boolean existsByCourseIdAndGroupIdAndAcademicYearAndSemester(
      UUID courseId, UUID groupId, int academicYear, Semester semester);

  /** The distinct courses actually assigned to the given groups within the given semesters. */
  @Query(
      """
      select distinct a.course from JCourseAssignment a
      where a.group.id in :groupIds
        and a.semester in :semesters
        and a.course.isDeleted = false
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
      order by a.academicYear desc
      """)
  @EntityGraph(attributePaths = {"course"})
  List<JCourseAssignment> findByCourseIdAndSemesterInAndGroupIdIn(
      @Param("courseId") UUID courseId,
      @Param("semesters") Collection<Semester> semesters,
      @Param("groupIds") Collection<UUID> groupIds);
}
