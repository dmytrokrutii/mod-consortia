package org.folio.consortia.service;

import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.domain.dto.TenantCollection;
import org.folio.consortia.domain.entity.TenantEntity;

import java.util.UUID;

public interface TenantService {

  /**
   * Gets tenant collection based on consortiumId.
   *
   * @param consortiumId  the consortiumId
   * @param limit  the limit
   * @param offset the offset
   * @return tenant collection
   */
  TenantCollection get(UUID consortiumId, Integer offset, Integer limit);

  /**
   * Inserts single tenant based on consortiumId.
   *
   * @param consortiumId  the consortiumId
   * @param tenantDto  the tenantDto
   * @param adminUserId the id of admin_user
   * @return tenantDto
   */
  Tenant save(UUID consortiumId, UUID adminUserId, Tenant tenantDto);

  /**
   * Updates single tenant based on consortiumId.
   *
   * @param consortiumId  the consortiumId
   * @param tenantId the tenantId
   * @param tenantDto  the tenantDto
   * @return tenantDto
   */
  Tenant update(UUID consortiumId, String tenantId, Tenant tenantDto);

  /**
   * Deletes single tenant based on consortiumId.
   * @param consortiumId the consortiumId
   * @param tenantId the tenantId
   */
  void delete(UUID consortiumId, String tenantId);

  /**
   * Gets tenant entity based on tenantId.
   *
   * @param tenantId the tenantId
   * @return tenant Entity
   */
  TenantEntity getByTenantId(String tenantId);

  /**
   * Gets central tenant id from db
   * @return central tenant id
   */
  String getCentralTenantId();
}
