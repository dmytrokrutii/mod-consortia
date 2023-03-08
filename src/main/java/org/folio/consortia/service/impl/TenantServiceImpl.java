package org.folio.consortia.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.consortia.service.TenantService;
import org.folio.pv.domain.dto.TenantCollection;
import org.folio.consortia.repository.TenantRepository;
import org.folio.consortia.repository.entity.Tenant;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {
  private final TenantRepository repository;

  @Transactional(readOnly = true)
  @Override
  public TenantCollection get(Integer offset, Integer limit) {
    var result = new TenantCollection();
    Page<Tenant> page = repository.findAll(new OffsetRequest(offset, limit));
    result.setTenants(page.map(this::entityToDto).getContent());
    result.setTotalRecords((int) page.getTotalElements());

    return result;
  }

  private org.folio.pv.domain.dto.Tenant entityToDto(Tenant entity) {
    var result = new org.folio.pv.domain.dto.Tenant();

    result.setTenantId(entity.getId());
    result.setTenantName(entity.getName());

    return result;
  }

}
