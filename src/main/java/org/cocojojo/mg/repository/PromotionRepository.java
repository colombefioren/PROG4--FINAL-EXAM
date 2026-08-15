package org.cocojojo.mg.repository;

import java.util.UUID;
import org.cocojojo.mg.repository.model.JPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRepository extends JpaRepository<JPromotion, UUID> {}
