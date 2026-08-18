package org.cocojojo.mg.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.repository.model.JStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRepository extends JpaRepository<JStudent, UUID> {
  @Query(
      value =
          "select std from \"student\" where std like concat(:prefix, '%') order by std desc limit"
              + " 1",
      nativeQuery = true)
  Optional<String> findLastStdStartingWith(@Param("prefix") String prefix);

  List<JStudent> findByPromotionIdOrderByLastnameAscFirstnameAsc(UUID promotionId);
  
  @Modifying
  @Query(value = "update \"user\" set \"is_deleted\" = true where \"id\" = :id", nativeQuery = true)
  void softDeleteById(@Param("id") UUID id);
}
