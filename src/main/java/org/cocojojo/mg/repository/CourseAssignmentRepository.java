package org.cocojojo.mg.repository;

import java.util.UUID;
import org.cocojojo.mg.repository.model.JCourseAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseAssignmentRepository extends JpaRepository<JCourseAssignment, UUID> {}