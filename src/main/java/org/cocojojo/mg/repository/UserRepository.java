package org.cocojojo.mg.repository;

import java.util.Optional;
import java.util.UUID;
import org.cocojojo.mg.repository.model.JUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<JUser, UUID> {
  Optional<JUser> findByEmailIgnoreCase(String email);

  /**
   * Native query on purpose: @SQLRestriction hides soft-deleted users from derived/JPQL queries.
   */
  @Query(
      value = "select is_deleted from \"user\" where lower(email) = lower(:email)",
      nativeQuery = true)
  Optional<Boolean> findDeletedFlagByEmailIgnoreCase(@Param("email") String email);
}
