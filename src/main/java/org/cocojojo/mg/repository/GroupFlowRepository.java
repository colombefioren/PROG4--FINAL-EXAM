package org.cocojojo.mg.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.repository.model.JGroupFlow;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupFlowRepository extends JpaRepository<JGroupFlow, UUID> {

  List<JGroupFlow> findByStudentId(UUID studentId);

  List<JGroupFlow> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

  Optional<JGroupFlow> findFirstByStudentIdOrderByCreatedAtDesc(UUID studentId);

  List<JGroupFlow> findByGroupId(UUID groupId);

  List<JGroupFlow> findByGroupPromotionId(UUID promotionId);

  @EntityGraph(attributePaths = {"group"})
  List<JGroupFlow> findByStudentIdIn(Collection<UUID> studentIds);
}
