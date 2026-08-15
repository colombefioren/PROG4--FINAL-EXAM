package org.cocojojo.mg.repository;

import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.repository.model.JGroupFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupFlowRepository extends JpaRepository<JGroupFlow, UUID> {

  Optional<JGroupFlow> findFirstByStudentIdOrderByCreatedAtDesc(UUID studentId);
}
