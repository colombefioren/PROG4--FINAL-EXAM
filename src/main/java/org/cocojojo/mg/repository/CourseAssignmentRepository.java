package org.cocojojo.mg.repository;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseAssignmentRepository extends JpaRepository<JCourseAssignment, UUID> {

  List<JCourseAssignment> findByGroupId(UUID groupId);

  List<JCourseAssignment> findByTeachers_Id(UUID teacherId);

  List<JCourseAssignment> findByCourseId(UUID courseId);

  List<JCourseAssignment> findByAcademicYear(int academicYear);

  List<JCourseAssignment> findByGroupIdAndTeachers_Id(UUID groupId, UUID teacherId);

  List<JCourseAssignment> findByGroupIdAndAcademicYearAndSemester(
      UUID groupId, int academicYear, Semester semester);

  boolean existsByCourseIdAndGroupIdAndAcademicYearAndSemester(
      UUID courseId, UUID groupId, int academicYear, Semester semester);
}
