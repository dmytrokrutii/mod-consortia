package org.folio.consortia.controller;

import lombok.RequiredArgsConstructor;
import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.rest.resource.ConsortiumIdApi;
import org.folio.consortia.service.TenantService;
import org.folio.consortia.domain.dto.TenantCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/consortia")
@RequiredArgsConstructor
public class TenantController implements ConsortiumIdApi {
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
}
