package org.folio.consortia.service;

import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.domain.dto.TenantCollection;

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
   * @return tenantDto
   */
  Tenant save(UUID consortiumId, Tenant tenantDto);

  /**
   * Updates single tenant based on consortiumId.
   *
   * @param consortiumId  the consortiumId
   * @param tenantId the tenantId
   * @param tenantDto  the tenantDto
   * @return tenantDto
   */
  Tenant update(UUID consortiumId, String tenantId, Tenant tenantDto);
}
