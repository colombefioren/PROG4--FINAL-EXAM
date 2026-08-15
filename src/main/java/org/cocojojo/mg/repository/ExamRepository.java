package org.cocojojo.mg.repository;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.repository.model.JExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends JpaRepository<JExam, UUID> {

  List<JExam> findByCourseAssignmentId(UUID courseAssignmentId);
}
