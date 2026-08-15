package org.cocojojo.mg.repository;

import java.util.UUID;
import org.cocojojo.mg.repository.model.JGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeRepository extends JpaRepository<JGrade, UUID> {}
