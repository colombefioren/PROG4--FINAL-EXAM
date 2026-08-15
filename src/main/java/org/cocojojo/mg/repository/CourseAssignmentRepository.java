package org.cocojojo.mg.repository;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseAssignmentRepository extends JpaRepository<JCourseAssignment, UUID> {

  @Query(
      """
      select ca from JCourseAssignment ca
      where (:groupId is null or ca.group.id = :groupId)
        and (:teacherId is null
          or exists (select t from ca.teachers t where t.id = :teacherId))
        and (:courseId is null or ca.course.id = :courseId)
        and (:academicYear is null or ca.academicYear = :academicYear)
      """)
  List<JCourseAssignment> search(
      @Param("groupId") UUID groupId,
      @Param("teacherId") UUID teacherId,
      @Param("courseId") UUID courseId,
      @Param("academicYear") Integer academicYear);

  List<JCourseAssignment> findByGroupIdAndAcademicYearAndSemester(
      UUID groupId, int academicYear, Semester semester);

  boolean existsByCourseIdAndGroupIdAndAcademicYearAndSemester(
      UUID courseId, UUID groupId, int academicYear, Semester semester);
}
