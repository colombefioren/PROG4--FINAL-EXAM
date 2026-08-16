package org.cocojojo.mg.repository;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.repository.model.JCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<JCourse, UUID> {

  List<JCourse> findByStudentLevel(StudentLevel studentLevel);

  List<JCourse> findByStudentLevelInAndTrack(List<StudentLevel> studentLevels, Track track);
}
