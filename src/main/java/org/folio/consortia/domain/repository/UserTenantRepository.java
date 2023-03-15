package org.folio.consortia.domain.repository;

import org.folio.consortia.domain.entity.UserTenantEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTenantRepository extends JpaRepository<UserTenantEntity, UUID> {
  Page<UserTenantEntity> findByUserId(UUID userId, Pageable pageable);

  @Query("SELECT ut FROM UserTenantEntity ut WHERE ut.username= ?1 AND ut.tenant.id= ?2")
  Optional<UserTenantEntity> findByUsernameAndTenantId(String username, String tenantId);
}
