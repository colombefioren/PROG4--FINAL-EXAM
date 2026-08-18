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

  /**
   * Native query on purpose: the derived query would inherit @SQLRestriction and skip soft-deleted
   * students, whose std is still occupied and must not be reused.
   */
  @Query(
      value =
          "select std from \"student\" where std like concat(:prefix, '%') order by std desc limit"
              + " 1",
      nativeQuery = true)
  Optional<String> findLastStdStartingWith(@Param("prefix") String prefix);

  List<JStudent> findByPromotionIdOrderByLastnameAscFirstnameAsc(UUID promotionId);

  /**
   * Native query on purpose: the @SQLDelete soft delete on JUser only covers the "user" row, but a
   * repository.delete(student) would also hard-delete the joined "student" row, breaking FKs from
   * soft-deleted grades. Soft-deleting the parent row keeps the joined row and its FKs intact.
   */
  @Modifying
  @Query(value = "update \"user\" set \"is_deleted\" = true where \"id\" = :id", nativeQuery = true)
  void softDeleteById(@Param("id") UUID id);
}
