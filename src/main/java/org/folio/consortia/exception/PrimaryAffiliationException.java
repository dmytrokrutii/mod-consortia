package org.folio.consortia.exception;

public class PrimaryAffiliationException extends RuntimeException {
  private static final String HAS_PRIMARY_AFFILIATION = "User with id [%s] has primary affiliation with tenant [%s]";

  public PrimaryAffiliationException(String userId, String tenantId) {
    super(String.format(HAS_PRIMARY_AFFILIATION, userId, tenantId));
  }

  public PrimaryAffiliationException(String errorMsg) {
    super(String.format(errorMsg));
  }
}
