package org.folio.consortia.controller;

import lombok.RequiredArgsConstructor;
import org.folio.consortia.service.TenantService;
import org.folio.consortia.domain.dto.TenantCollection;
import org.folio.consortia.rest.resource.TenantsApi;
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
