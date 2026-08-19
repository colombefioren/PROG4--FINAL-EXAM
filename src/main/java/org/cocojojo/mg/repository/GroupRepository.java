package org.cocojojo.mg.repository;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.repository.model.JGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<JGroup, UUID> {

  List<JGroup> findByPromotionId(UUID promotionId);

  Page<JGroup> findByPromotionId(UUID promotionId, Pageable pageable);
}
