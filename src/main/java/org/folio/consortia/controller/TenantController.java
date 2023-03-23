package org.folio.consortia.controller;

import lombok.RequiredArgsConstructor;
import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.rest.resource.TenantsApi;
import org.folio.consortia.service.TenantService;
import org.folio.consortia.domain.dto.TenantCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/consortia/{consortiumId}")
@RequiredArgsConstructor
public class TenantController implements TenantsApi {
  @Autowired
  private final TenantService service;

  @Override
  public ResponseEntity<TenantCollection> getTenants(UUID consortiumId, Integer offset, Integer limit) {
    return ResponseEntity.ok(service.get(consortiumId, offset, limit));
  }

  @Override
  public ResponseEntity<Tenant> saveTenant(UUID consortiumId, Tenant tenant) {
    return ResponseEntity.ok(service.save(consortiumId, tenant));
  }

  @Override
  @PutMapping("/tenants/{tenantId}")
  public ResponseEntity<Tenant> updateTenant(UUID consortiumId, String tenantId, Tenant tenant) {
    return ResponseEntity.ok(service.update(consortiumId, tenantId, tenant));
  }
}
