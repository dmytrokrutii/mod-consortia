package org.folio.consortia.service;

import org.folio.consortia.domain.dto.SyncPrimaryAffiliationBody;

import java.util.UUID;

public interface SyncPrimaryAffiliationService {
  void syncPrimaryAffiliations(UUID consortiumId, String syncPrimaryAffiliationBody);


  /**
   * Create primary affiliation for user
   *
   * @param consortiumId               - consortium unique identifier
   * @param syncPrimaryAffiliationBody - consortia tenant record
   */
  void createPrimaryUserAffiliations(UUID consortiumId, SyncPrimaryAffiliationBody syncPrimaryAffiliationBody);
}
