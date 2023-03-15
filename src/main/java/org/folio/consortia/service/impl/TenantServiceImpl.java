package org.folio.consortia.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.domain.dto.TenantCollection;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.domain.repository.TenantRepository;
import org.folio.consortia.service.TenantService;
import org.folio.spring.data.OffsetRequest;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

  private final TenantRepository repository;
  private final ConversionService converter;

  @Override
  public TenantCollection get(Integer offset, Integer limit) {

    var result = new TenantCollection();
    Page<TenantEntity> page = repository.findAll(new OffsetRequest(offset, limit));
    result.setTenants(page.map(o -> converter.convert(o, Tenant.class)).getContent());
    result.setTotalRecords((int) page.getTotalElements());

    return result;
  }

}
