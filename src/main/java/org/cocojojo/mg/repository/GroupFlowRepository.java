package org.cocojojo.mg.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.repository.model.JGroupFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupFlowRepository extends JpaRepository<JGroupFlow, UUID> {

  List<JGroupFlow> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

  List<JGroupFlow> findByStudentId(UUID studentId);

  Optional<JGroupFlow> findFirstByStudentIdOrderByCreatedAtDesc(UUID studentId);

  List<JGroupFlow> findByGroupId(UUID groupId);

  List<JGroupFlow> findByGroupPromotionId(UUID promotionId);
}
