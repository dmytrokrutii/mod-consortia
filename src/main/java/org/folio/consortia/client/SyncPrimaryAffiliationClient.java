package org.folio.consortia.client;

import org.folio.consortia.domain.dto.SyncPrimaryAffiliationBody;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "consortia", configuration = FeignClientConfiguration.class)
public interface SyncPrimaryAffiliationClient {
  @PostMapping(value = "/{consortiumId}/tenants/{tenantId}/sync-primary-affiliations")
  void syncPrimaryAffiliations(@PathVariable("consortiumId") String consortiumId, @PathVariable("tenantId") String tenantId);

  @PostMapping(value = "/{consortiumId}/tenants/{tenantId}/create-primary-affiliations", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  SyncPrimaryAffiliationBody savePrimaryAffiliations(@RequestBody SyncPrimaryAffiliationBody syncPrimaryAffiliationBody,
                                                     @PathVariable("consortiumId") String consortiumId, @PathVariable("tenantId") String tenantId);

}
