package org.folio.consortia.service;

import org.folio.consortia.domain.dto.UserTenant;
import org.folio.consortia.domain.dto.UserTenantCollection;

import java.util.UUID;

/**
 * Service to work with user tenant associations, it provides ability to add association between user and tenant
 * and search tenants that user assigned to.
 *
 * In methods internal implementation there is checking if consortia exists without joins to the consortium table.
 * Also, user_tenant table does not contain field consortiumId because consortium table will always contain only single
 * consortium. Other consortiums will be stored in separate DB schema.
 */
public interface UserTenantService {

  /**
   * Get user tenant associations collection by query based on consortiumId.
   *
   * @param consortiumId the consortiumId
   * @param offset the offset
   * @param limit  the limit
   * @return the user tenant associations collection
   */
  UserTenantCollection get(UUID consortiumId, Integer offset, Integer limit);

  /**
   * Get user tenant associations collection by user id based on consortiumId.
   *
   * @param consortiumId the consortiumId
   * @param userId the user id
   * @return the user tenant associations collection
   */
  UserTenantCollection getByUserId(UUID consortiumId, UUID userId, Integer offset, Integer limit);

  /**
   * Get a user tenant associations collection by username based on consortiumId.
   *
   * @param consortiumId the consortiumId
   * @param username the username
   * @param tenantId the tenant id
   * @return the user tenant associations collection
   */
  UserTenantCollection getByUsernameAndTenantId(UUID consortiumId, String username, String tenantId);

  /**
   * Get user tenant association by id based on consortiumId.
   *
   * @param consortiumId the consortiumId
   * @param id the id
   * @return the user tenant association
   */
  UserTenant getById(UUID consortiumId, UUID id);

  /**
   * Inserts single user_tenant based on consortiumId.
   *
   * @param consortiumId  the consortiumId
   * @param userTenantDto  the tenantDto
   * @return userTenantDto
   */
  UserTenant save(UUID consortiumId, UserTenant userTenantDto);
}
