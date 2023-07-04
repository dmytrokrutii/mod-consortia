package org.folio.consortia.service.impl;

import static org.folio.consortia.repository.SharingInstanceRepository.Specifications.constructSpecification;

import java.util.Objects;
import java.util.UUID;

import org.folio.consortia.domain.dto.SharingInstance;
import org.folio.consortia.domain.dto.SharingInstanceCollection;
import org.folio.consortia.domain.dto.Status;
import org.folio.consortia.domain.entity.SharingInstanceEntity;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.folio.consortia.repository.SharingInstanceRepository;
import org.folio.consortia.service.ConsortiumService;
import org.folio.consortia.service.SharingInstanceService;
import org.folio.consortia.service.TenantService;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class SharingInstanceServiceImpl implements SharingInstanceService {

  private final SharingInstanceRepository sharingInstanceRepository;
  private final ConsortiumService consortiumService;
  private final TenantService tenantService;
  private final ConversionService converter;

  @Override
  public SharingInstance getById(UUID consortiumId, UUID actionId) {
    log.debug("getById:: Trying to get sharingInstance by consortiumId: {} and action id: {}", consortiumId, actionId);
    SharingInstanceEntity sharingInstanceEntity = sharingInstanceRepository.findById(actionId).
      orElseThrow(() -> new ResourceNotFoundException("actionId", String.valueOf(actionId)));
    log.info("getById:: sharingInstance object with id: {} was successfully retrieved", sharingInstanceEntity.getId());
    return converter.convert(sharingInstanceEntity, SharingInstance.class);
  }

  @Override
  @Transactional
  public SharingInstance start(UUID consortiumId, SharingInstance sharingInstance) {
    log.debug("start:: Trying to start instance sharing: {} for consortium: {}", sharingInstance.getInstanceIdentifier(), consortiumId);
    consortiumService.checkConsortiumExistsOrThrow(consortiumId);
    checkTenantsExistAndContainCentralTenantOrThrow(sharingInstance.getSourceTenantId(), sharingInstance.getTargetTenantId());

    SharingInstanceEntity savedSharingInstance = sharingInstanceRepository.save(toEntity(sharingInstance));
    log.info("start:: sharingInstance with id: {}, instanceId: {}, sourceTenantId: {}, targetTenantId: {} successfully saved in db",
      savedSharingInstance.getId(), savedSharingInstance.getInstanceId(), savedSharingInstance.getSourceTenantId(), savedSharingInstance.getTargetTenantId());
    return converter.convert(savedSharingInstance, SharingInstance.class);
  }

  @Override
  public SharingInstanceCollection getSharingInstances(UUID consortiumId, UUID instanceIdentifier, String sourceTenantId,
      String targetTenantId, Status status, Integer offset, Integer limit) {
    log.debug("getSharingInstances:: parameters consortiumId: {}, instanceIdentifier: {}, sourceTenantId: {}, targetTenantId: {}, status: {}.",
      consortiumId, instanceIdentifier, sourceTenantId, targetTenantId, status);
    consortiumService.checkConsortiumExistsOrThrow(consortiumId);
    var specification = constructSpecification(instanceIdentifier, sourceTenantId, targetTenantId, status);

    var sharingInstancePage = sharingInstanceRepository.findAll(specification, PageRequest.of(offset, limit));
    var result = new SharingInstanceCollection();
    result.setSharingInstances(sharingInstancePage.stream().map(o -> converter.convert(o, SharingInstance.class)).toList());
    result.setTotalRecords((int) sharingInstancePage.getTotalElements());
    log.info("getSharingInstances:: total number of matched sharingInstances: {}.", result.getTotalRecords());
    return result;
  }

  private void checkTenantsExistAndContainCentralTenantOrThrow(String sourceTenantId, String targetTenantId) {
    // both tenants should exist in the consortium
    tenantService.checkTenantExistsOrThrow(sourceTenantId);
    tenantService.checkTenantExistsOrThrow(targetTenantId);

    // at least one of the tenants should be 'centralTenant'
    String centralTenantId = tenantService.getCentralTenantId();
    if (Objects.equals(centralTenantId, sourceTenantId) || Objects.equals(centralTenantId, targetTenantId)) {
      return;
    }
    throw new IllegalArgumentException("Both 'sourceTenantId' and 'targetTenantId' cannot be member tenants.");
  }

  private SharingInstanceEntity toEntity(SharingInstance dto) {
    SharingInstanceEntity entity = new SharingInstanceEntity();
    entity.setId(UUID.randomUUID());
    entity.setInstanceId(dto.getInstanceIdentifier());
    entity.setSourceTenantId(dto.getSourceTenantId());
    entity.setTargetTenantId(dto.getTargetTenantId());
    entity.setStatus(Status.IN_PROGRESS);
    return entity;
  }
}
