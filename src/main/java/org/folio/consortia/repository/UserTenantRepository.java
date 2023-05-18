package org.folio.consortia.repository;

import org.folio.consortia.domain.entity.UserTenantEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTenantRepository extends JpaRepository<UserTenantEntity, UUID> {
  Page<UserTenantEntity> findByUserId(UUID userId, Pageable pageable);

  @Query("SELECT ut FROM UserTenantEntity ut WHERE ut.username= ?1 AND ut.tenant.id= ?2")
  Optional<UserTenantEntity> findByUsernameAndTenantId(String username, String tenantId);

  @Query("SELECT ut FROM UserTenantEntity ut WHERE ut.userId= ?1 AND ut.tenant.id= ?2")
  Optional<UserTenantEntity> findByUserIdAndTenantId(UUID userId, String tenantId);

  boolean existsByTenantId(String tenantId);

  @Query("SELECT ut FROM UserTenantEntity ut WHERE ut.userId= ?1 AND ut.isPrimary= ?2")
  Optional<UserTenantEntity> findByUserIdAndIsPrimary(UUID userId, Boolean isPrimary);

  @Modifying
  @Query("DELETE FROM UserTenantEntity ut WHERE ut.userId= ?1 AND ut.tenant.id= ?2")
  void deleteByUserIdAndTenantId(UUID userId, String tenantId);

  @Modifying
  @Query("DELETE FROM UserTenantEntity ut WHERE ut.userId= ?1 AND ut.isPrimary= true")
  void deleteByUserIdAndIsPrimaryTrue(UUID userId);

  @Query("SELECT ut FROM UserTenantEntity ut WHERE ut.userId NOT IN (SELECT ut.userId FROM UserTenantEntity ut WHERE ut.userId= ?1 AND ut.isPrimary=true) AND ut.userId= ?1")
  List<UserTenantEntity> getByUserIdAndIsPrimaryFalse(UUID userId);

  @Modifying
  @Query("DELETE FROM UserTenantEntity ut WHERE ut.userId NOT IN (SELECT ut.userId FROM UserTenantEntity ut WHERE ut.userId= ?1 AND ut.isPrimary=true) AND ut.userId= ?1")
  void deleteByUserIdAndIsPrimaryFalse(UUID userId);
}
