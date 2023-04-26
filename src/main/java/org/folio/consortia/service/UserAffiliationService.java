package org.folio.consortia.service;

public interface UserAffiliationService {

  /**
   * Create primary affiliation for user
   * @param userEvent - user event object from kafka
   */
  void createPrimaryUserAffiliation(String userEvent);

  /**
   * Delete primary affiliation for user
   * @param userEvent - user event object from kafka
   */
  void deletePrimaryUserAffiliation(String userEvent);
}
