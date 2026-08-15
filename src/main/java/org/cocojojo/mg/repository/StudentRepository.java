package org.cocojojo.mg.repository;

import java.util.UUID;
import org.cocojojo.mg.repository.model.JStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<JStudent, UUID> {}
