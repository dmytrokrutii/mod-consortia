package org.folio.consortia.repository;

import java.util.Set;
import java.util.UUID;

import org.folio.consortia.domain.entity.SharingSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SharingSettingRepository  extends JpaRepository<SharingSettingEntity, UUID>{
  @Query("SELECT st.tenantId FROM SharingSettingEntity st WHERE st.settingId = ?1")
  Set<String> findTenantsBySettingId(UUID settingId);
}
