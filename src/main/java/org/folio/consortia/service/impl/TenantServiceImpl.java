package org.folio.consortia.service.impl;

import static org.folio.consortia.utils.HelperUtils.checkIdenticalOrThrow;

import java.util.UUID;

import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.domain.dto.TenantCollection;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.exception.ResourceAlreadyExistException;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.folio.consortia.repository.TenantRepository;
import org.folio.consortia.repository.UserTenantRepository;
import org.folio.consortia.service.ConsortiumService;
import org.folio.consortia.service.TenantService;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

  private static final String TENANTS_IDS_NOT_MATCHED_ERROR_MSG = "Request body tenantId and path param tenantId should be identical";
  private static final String TENANT_HAS_ACTIVE_USER_ASSOCIATIONS_ERROR_MSG = "Cannot delete tenant with ID {tenantId} because it has an association with a user. " +
    "Please remove the user association before attempting to delete the tenant.";
  private final TenantRepository tenantRepository;
  private final UserTenantRepository userTenantRepository;
  private final ConversionService converter;
  private final ConsortiumService consortiumService;

  @Override
  public TenantCollection get(UUID consortiumId, Integer offset, Integer limit) {
    TenantCollection result = new TenantCollection();
    consortiumService.checkConsortiumExistsOrThrow(consortiumId);
    Page<TenantEntity> page = tenantRepository.findByConsortiumId(consortiumId, PageRequest.of(offset, limit));
    result.setTenants(page.map(o -> converter.convert(o, Tenant.class)).getContent());
    result.setTotalRecords((int) page.getTotalElements());
    return result;
  }

  @Override
  public Tenant save(UUID consortiumId, Tenant tenantDto) {
    consortiumService.checkConsortiumExistsOrThrow(consortiumId);
    checkTenantNotExistsOrThrow(tenantDto.getId());
    TenantEntity entity = toEntity(consortiumId, tenantDto);
    TenantEntity tenantEntity = tenantRepository.save(entity);
    return converter.convert(tenantEntity, Tenant.class);
  }

  @Override
  public Tenant update(UUID consortiumId, String tenantId, Tenant tenantDto) {
    consortiumService.checkConsortiumExistsOrThrow(consortiumId);
    checkTenantExistsOrThrow(tenantId);
    checkIdenticalOrThrow(tenantId, tenantDto.getId(), TENANTS_IDS_NOT_MATCHED_ERROR_MSG);
    TenantEntity entity = toEntity(consortiumId, tenantDto);
    TenantEntity tenantEntity = tenantRepository.save(entity);
    return converter.convert(tenantEntity, Tenant.class);
  }

  @Override
  public void delete(UUID consortiumId, String tenantId) {
    consortiumService.checkConsortiumExistsOrThrow(consortiumId);
    checkTenantExistsOrThrow(tenantId);
    if (userTenantRepository.existsByTenantId(tenantId)) {
      throw new IllegalStateException(TENANT_HAS_ACTIVE_USER_ASSOCIATIONS_ERROR_MSG);
    }
    tenantRepository.deleteById(tenantId);
  }

  @Override
  public TenantEntity getByTenantId(String tenantId) {
      return tenantRepository.findById(tenantId)
        .orElse(null);
  }

  private void checkTenantNotExistsOrThrow(String tenantId) {
    if (tenantRepository.existsById(tenantId)) {
      throw new ResourceAlreadyExistException("id", tenantId);
    }
  }

  private void checkTenantExistsOrThrow(String tenantId) {
    if (!tenantRepository.existsById(tenantId)) {
      throw new ResourceNotFoundException("id", tenantId);
    }
  }

  private TenantEntity toEntity(UUID consortiumId, Tenant tenantDto) {
    TenantEntity entity = new TenantEntity();
    entity.setId(tenantDto.getId());
    entity.setName(tenantDto.getName());
    entity.setConsortiumId(consortiumId);
    return entity;
  }
}
