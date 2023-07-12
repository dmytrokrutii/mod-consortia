package org.folio.consortia.service.impl;

import static org.folio.consortia.repository.SharingInstanceRepository.Specifications.constructSpecification;
import static org.folio.consortia.utils.TenantContextUtils.prepareContextForTenant;

import java.util.Objects;
import java.util.UUID;

import org.folio.consortia.client.InventoryClient;
import org.folio.consortia.domain.dto.SharingInstance;
import org.folio.consortia.domain.dto.SharingInstanceCollection;
import org.folio.consortia.domain.dto.Status;
import org.folio.consortia.domain.entity.SharingInstanceEntity;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.folio.consortia.repository.SharingInstanceRepository;
import org.folio.consortia.service.ConsortiumService;
import org.folio.consortia.service.InventoryService;
import org.folio.consortia.service.SharingInstanceService;
import org.folio.consortia.service.TenantService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class SharingInstanceServiceImpl implements SharingInstanceService {
  private static final String FOLIO_SOURCE_VALUE = "folio";
  private static final String CONSORTIUM_FOLIO = "CONSORTIUM-FOLIO";
  private static final String CONSORTIUM_MARK = "CONSORTIUM-MARC";
  private static final String GET_INSTANCE_EXCEPTION_MSG = "Failed to get inventory instance with reason: %s";
  private static final String POST_INSTANCE_EXCEPTION_MSG = "Failed to post inventory instance with reason: %s";

  private final SharingInstanceRepository sharingInstanceRepository;
  private final ConsortiumService consortiumService;
  private final TenantService tenantService;
  private final ConversionService converter;
  private final InventoryService inventoryService;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;

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
    log.debug("start:: Trying to start instance sharing with instanceIdentifier: {}, consortiumId: {}", sharingInstance.getInstanceIdentifier(), consortiumId);
    consortiumService.checkConsortiumExistsOrThrow(consortiumId);

    String centralTenantId = tenantService.getCentralTenantId();
    String sourceTenantId = sharingInstance.getSourceTenantId();
    String targetTenantId = sharingInstance.getTargetTenantId();
    checkTenantsExistAndContainCentralTenantOrThrow(sourceTenantId, targetTenantId);

    if (Objects.equals(centralTenantId, sourceTenantId)) {
      JsonNode inventoryInstance;

      try (var context = new FolioExecutionContextSetter(prepareContextForTenant(sourceTenantId, folioModuleMetadata, folioExecutionContext))) {
        inventoryInstance = inventoryService.getById(sharingInstance.getInstanceIdentifier());
      } catch (Exception ex) {
        log.error("start:: error when getting instance by id: {}", sharingInstance.getInstanceIdentifier(), ex);
        return updateFieldsAndSaveInCaseOfException(sharingInstance, GET_INSTANCE_EXCEPTION_MSG, ex);
      }

      try (var context = new FolioExecutionContextSetter(prepareContextForTenant(targetTenantId, folioModuleMetadata, folioExecutionContext))) {
        String source = FOLIO_SOURCE_VALUE.equalsIgnoreCase(inventoryInstance.get("source").asText()) ? CONSORTIUM_FOLIO : CONSORTIUM_MARK;
        var updatedInventoryInstance = ((ObjectNode) inventoryInstance).put("source", source);
        inventoryService.saveInstance(updatedInventoryInstance);
      } catch (Exception ex) {
        log.error("start:: error when posting instance with id: {}", sharingInstance.getInstanceIdentifier(), ex);
        return updateFieldsAndSaveInCaseOfException(sharingInstance, POST_INSTANCE_EXCEPTION_MSG, ex);
      }

      sharingInstance.setStatus(Status.COMPLETE);
    } else {
      sharingInstance.setStatus(Status.IN_PROGRESS);
    }
    SharingInstanceEntity savedSharingInstance = sharingInstanceRepository.save(toEntity(sharingInstance));
    log.info("start:: sharingInstance with id: {}, instanceId: {}, sourceTenantId: {}, targetTenantId: {} has been saved with status: {}",
      savedSharingInstance.getId(), savedSharingInstance.getInstanceId(), sourceTenantId, targetTenantId, savedSharingInstance.getStatus());
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
    entity.setStatus(dto.getStatus());
    entity.setError(dto.getError());
    return entity;
  }

  private SharingInstance updateFieldsAndSaveInCaseOfException(SharingInstance sharingInstance, String message, Exception ex) {
    sharingInstance.setStatus(Status.ERROR);
    sharingInstance.setError(String.format(message, InventoryClient.getReason(ex)));
    var savedSharingInstance = sharingInstanceRepository.save(toEntity(sharingInstance));
    return converter.convert(savedSharingInstance, SharingInstance.class);
  }
}
