package org.folio.consortia.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.domain.dto.TenantCollection;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.domain.repository.TenantRepository;
import org.folio.consortia.exception.ResourceAlreadyExistException;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.folio.consortia.service.ConsortiumService;
import org.folio.consortia.service.TenantService;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.folio.consortia.utils.HelperUtils.checkIdenticalOrThrow;

@Service
@Log4j2
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {
  private static final String TENANTS_IDS_NOT_MATCHED_ERROR_MSG = "Request body tenantId and path param tenantId should be identical";

  private final TenantRepository repository;
  private final ConversionService converter;
  private final ConsortiumService consortiumService;

  @Override
  public TenantCollection get(UUID consortiumId, Integer offset, Integer limit) {
    TenantCollection result = new TenantCollection();
    consortiumService.checkConsortiumExistsOrThrow(consortiumId);
    Page<TenantEntity> page = repository.findByConsortiumId(consortiumId, PageRequest.of(offset, limit));
    result.setTenants(page.map(o -> converter.convert(o, Tenant.class)).getContent());
    result.setTotalRecords((int) page.getTotalElements());
    return result;
  }

  @Override
  public Tenant save(UUID consortiumId, Tenant tenantDto) {
    consortiumService.checkConsortiumExistsOrThrow(consortiumId);
    checkTenantNotExistsOrThrow(tenantDto.getId());
    TenantEntity entity = toEntity(consortiumId, tenantDto);
    TenantEntity tenantEntity = repository.save(entity);
    return converter.convert(tenantEntity, Tenant.class);
  }

  @Override
  public Tenant update(UUID consortiumId, String tenantId, Tenant tenantDto) {
    consortiumService.checkConsortiumExistsOrThrow(consortiumId);
    checkTenantExistsOrThrow(tenantId);
    checkIdenticalOrThrow(tenantId, tenantDto.getId(), TENANTS_IDS_NOT_MATCHED_ERROR_MSG);
    TenantEntity entity = toEntity(consortiumId, tenantDto);
    TenantEntity tenantEntity = repository.save(entity);
    return converter.convert(tenantEntity, Tenant.class);
  }

  private void checkTenantNotExistsOrThrow(String tenantId) {
    repository.findById(tenantId).ifPresent(s -> { throw new ResourceAlreadyExistException("id", tenantId); });
  }

  private TenantEntity checkTenantExistsOrThrow(String tenantId) {
    return repository.findById(tenantId).orElseThrow(() ->  new ResourceNotFoundException("tenantId", tenantId));
  }

  private TenantEntity toEntity(UUID consortiumId, Tenant tenantDto) {
    TenantEntity entity = new TenantEntity();
    entity.setId(tenantDto.getId());
    entity.setName(tenantDto.getName());
    entity.setConsortiumId(consortiumId);
    return entity;
  }
}
