package org.folio.consortia.controller;

import lombok.RequiredArgsConstructor;
import org.folio.pv.domain.dto.TenantCollection;
import org.folio.pv.rest.resource.TenantsApi;
import org.folio.consortia.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/consortia")
@RequiredArgsConstructor
public class TenantController implements TenantsApi {
  @Autowired
  private final TenantService service;

  @Override
  public ResponseEntity<TenantCollection> getTenants(Integer offset, Integer limit) {
    return ResponseEntity.ok(service.get(offset, limit));
  }
}
