package org.folio.consortia.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.domain.entity.TenantEntity;

public interface UserAffiliationAsyncService {

  /**
   * Create primary affiliation for user
   * @param consortiumId - consortium unique identifier
   * @param consortiaTenant - consortia tenant record
   * @param tenantDto - tenant DTO
   */
  CompletableFuture<Void> createPrimaryUserAffiliationsAsync(UUID consortiumId, TenantEntity consortiaTenant, Tenant tenantDto);

}
