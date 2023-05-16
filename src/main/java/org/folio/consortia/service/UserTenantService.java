package org.folio.consortia.service;

import org.folio.consortia.domain.dto.UserEvent;
import org.folio.consortia.domain.dto.UserTenant;
import org.folio.consortia.domain.dto.UserTenantCollection;
import org.folio.consortia.domain.entity.TenantEntity;

import java.util.UUID;

/**
 * Service to work with user tenant associations, it provides ability to add association between user and tenant
 * and search tenants that user assigned to.
 * <p>
 * In methods internal implementation there is checking if consortia exists without joins to the consortium table.
 * Also, user_tenant table does not contain field consortiumId because consortium table will always contain only single
 * consortium. Other consortiums will be stored in separate DB schema.
 */
public interface UserTenantService {

  /**
   * Get user tenant associations collection by query based on consortiumId.
   *
   * @param consortiumId the consortiumId
   * @param offset       the offset
   * @param limit        the limit
   * @return the user tenant associations collection
   */
  UserTenantCollection get(UUID consortiumId, Integer offset, Integer limit);

  /**
   * Get user tenant associations collection by user id based on consortiumId.
   *
   * @param consortiumId the consortiumId
   * @param userId       the user id
   * @return the user tenant associations collection
   */
  UserTenantCollection getByUserId(UUID consortiumId, UUID userId, Integer offset, Integer limit);

  /**
   * Get a user tenant associations collection by username based on consortiumId.
   *
   * @param consortiumId the consortiumId
   * @param username     the username
   * @param tenantId     the tenant id
   * @return the user tenant associations collection
   */
  UserTenantCollection getByUsernameAndTenantId(UUID consortiumId, String username, String tenantId);

  /**
   * Get user tenant association by id based on consortiumId.
   *
   * @param consortiumId the consortiumId
   * @param id           the id
   * @return the user tenant association
   */
  UserTenant getById(UUID consortiumId, UUID id);

  /**
   * Inserts single user_tenant based on consortiumId.
   *
   * @param consortiumId  the consortiumId
   * @param userTenantDto the tenantDto
   * @return userTenantDto
   */
  UserTenant save(UUID consortiumId, UserTenant userTenantDto);

  /**
   * Inserts single user_tenant based on kafka userEventDto.
   *
   * @param consortiumId    the consortiumId
   * @param consortiaTenant the consortiaTenant
   * @param userEventDto    the kafka userEventDto
   * @return userTenantDto
   */
  UserTenant createPrimaryUserTenantAffiliation(UUID consortiumId, TenantEntity consortiaTenant, UserEvent userEventDto);

  /**
   * Deletes user_tenant by userId and tenantId.
   *
   * @param consortiumId id of consortium
   * @param tenantId     id of tenant
   * @param userId       id of user
   */
  void deleteByUserIdAndTenantId(UUID consortiumId, String tenantId, UUID userId);

  /**
   * Updates user_tenant based on consortiumId and userTenantDto.
   *
   * @param consortiumId id of consortium
   * @param primary      userTenantDto
   * @return userTenantDto
   */
  UserTenant update(UUID consortiumId, UserTenant primary);

  /**
   * Check if user has primary affiliation.
   *
   * @param userId id of user in user_tenant table
   * @return true if user has primary affiliation
   */
  boolean checkUserIfHasPrimaryAffiliationByUserId(UUID consortiumId, String userId);

  /**
   * Delete primary user tenant affiliation.
   *
   * @param userId id of user in user_tenant table
   */
  void deletePrimaryUserTenantAffiliation(UUID userId);
}
