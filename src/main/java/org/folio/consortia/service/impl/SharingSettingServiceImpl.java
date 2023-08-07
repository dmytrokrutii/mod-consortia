package org.folio.consortia.service.impl;

import static org.folio.consortia.utils.HelperUtils.CONSORTIUM_SETTING_SOURCE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.consortia.config.FolioExecutionContextHelper;
import org.folio.consortia.domain.dto.PublicationRequest;
import org.folio.consortia.domain.dto.SharingSettingDeleteResponse;
import org.folio.consortia.domain.dto.SharingSettingRequest;
import org.folio.consortia.domain.dto.SharingSettingResponse;
import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.domain.dto.TenantCollection;
import org.folio.consortia.domain.entity.SharingSettingEntity;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.folio.consortia.repository.SharingSettingRepository;
import org.folio.consortia.service.ConsortiumService;
import org.folio.consortia.service.PublicationService;
import org.folio.consortia.service.SharingSettingService;
import org.folio.consortia.service.TenantService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class SharingSettingServiceImpl implements SharingSettingService {
  private final SharingSettingRepository sharingSettingRepository;
  private final TenantService tenantService;
  private final ConsortiumService consortiumService;
  private final FolioExecutionContextHelper contextHelper;
  private final PublicationService publicationService;
  private final FolioExecutionContext folioExecutionContext;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public SharingSettingResponse start(UUID consortiumId, SharingSettingRequest sharingSettingRequest) {
    UUID settingId = sharingSettingRequest.getSettingId();
    log.debug("start:: Trying to share setting with consortiumId: {}, sharing settingId: {}", consortiumId, settingId);
    // validations for consortium id and equals between payload and settingId
    consortiumService.checkConsortiumExistsOrThrow(consortiumId);
    checkEqualsOfPayloadIdWithSettingId(sharingSettingRequest);

    Set<String> settingTenants = sharingSettingRepository.findTenantsBySettingId(settingId);
    TenantCollection allTenants = tenantService.getAll(consortiumId);
    PublicationRequest publicationPostRequest = createPublicationRequestForSetting(sharingSettingRequest, HttpMethod.POST.toString());
    PublicationRequest publicationPutRequest = createPublicationRequestForSetting(sharingSettingRequest, HttpMethod.PUT.toString());

    // By traverse through all tenants in db,
    // we will add tenant to put method publication tenant list, if it exists in setting tenant associations
    // otherwise, we will add it to post method publication tenant list and save this association to sharing_tenant table
    List<SharingSettingEntity> sharingSettingEntityList = new ArrayList<>();
    for (Tenant tenant : allTenants.getTenants()) {
      if (settingTenants.contains(tenant.getId())) {
        publicationPutRequest.getTenants().add(tenant.getId());
        log.info("start:: tenant={} added to publication update request for setting={}", tenant.getId(), settingId);
      } else {
        publicationPostRequest.getTenants().add(tenant.getId());
        log.info("start:: tenant={} added to publication create request for setting={}", tenant.getId(), settingId);
        sharingSettingEntityList.add(createSharingSettingEntityFromRequest(sharingSettingRequest, tenant.getId()));
      }
    }
    log.info("start:: tenants with size: {} successfully added to appropriate publication request for setting: {}",
      allTenants.getTotalRecords(), settingId);
    sharingSettingRepository.saveAll(sharingSettingEntityList);
    log.info("start:: The Sharing Settings for settingId '{}' and '{}' unique tenant(s) were successfully saved to the database",
      sharingSettingRequest.getSettingId(), publicationPostRequest.getTenants().size());

    JsonNode payload = objectMapper.convertValue(sharingSettingRequest.getPayload(), JsonNode.class);
    var updatedPayload = ((ObjectNode) payload).set("source", new TextNode(CONSORTIUM_SETTING_SOURCE));
    publicationPostRequest.setPayload(updatedPayload);
    publicationPutRequest.setPayload(updatedPayload);
    log.info("start:: set source as '{}' in payload of setting: {}", updatedPayload.get("source"), settingId);

    // we create PC request with POST and PUT Http method to create settings as a consortia-system-user
    try (var ignored = new FolioExecutionContextSetter(contextHelper.getSystemUserFolioExecutionContext(folioExecutionContext.getTenantId()))) {
      UUID createSettingsPcId = publishRequest(consortiumId, publicationPostRequest);
      UUID updateSettingsPcId = publishRequest(consortiumId, publicationPutRequest);
      return new SharingSettingResponse()
        .createSettingsPCId(createSettingsPcId)
        .updateSettingsPCId(updateSettingsPcId);
    }
  }

  @Override
  @Transactional
  @SneakyThrows
  public SharingSettingDeleteResponse delete(UUID consortiumId, UUID settingId, SharingSettingRequest sharingSettingRequest) {
    log.debug("start:: Trying to delete sharing setting with consortiumId: {}, sharing settingId: {}", consortiumId, settingId);
    validateSharingSettingRequestOrThrow(settingId, sharingSettingRequest);
    consortiumService.checkConsortiumExistsOrThrow(consortiumId);
    Set<String> settingTenants = sharingSettingRepository.findTenantsBySettingId(settingId);
    TenantCollection allTenants = tenantService.getAll(consortiumId);
    PublicationRequest publicationDeleteRequest = createPublicationRequestForSetting(sharingSettingRequest, HttpMethod.DELETE.toString());

    // By traverse through all tenants in db,
    // we will add tenant to delete request publication tenant list, if it exists in setting tenant associations
    for (Tenant tenant : allTenants.getTenants()) {
      if (settingTenants.contains(tenant.getId())) {
        publicationDeleteRequest.getTenants().add(tenant.getId());
        log.info("start:: tenant={} added to publication delete request for setting={}", tenant.getId(), settingId);
      }
    }
    log.info("start:: tenants with size: {} successfully added to appropriate publication request for setting: {}", allTenants.getTotalRecords(), settingId);
    sharingSettingRepository.deleteBySettingId(settingId);
    log.info("start:: The Sharing Settings for settingId '{}' and '{}' unique tenant(s) were successfully deleted from the database", sharingSettingRequest.getSettingId(), publicationDeleteRequest.getTenants().size());

    // we create PC request with POST and PUT Http method to create settings as a consortia-system-user
    try (var ignored = new FolioExecutionContextSetter(contextHelper.getSystemUserFolioExecutionContext(folioExecutionContext.getTenantId()))) {
      UUID pcId = publishRequest(consortiumId, publicationDeleteRequest);
      return new SharingSettingDeleteResponse()
        .pcId(pcId);
    }
  }

  private void validateSharingSettingRequestOrThrow(UUID settingId, SharingSettingRequest sharingSettingRequest) {
    if (ObjectUtils.notEqual(sharingSettingRequest.getSettingId(), settingId)) {
      throw new IllegalArgumentException("Mismatch id in path to settingId in request body");
    }
    if (!sharingSettingRepository.existsBySettingId(settingId)) {
      throw new ResourceNotFoundException("settingId", String.valueOf(settingId));
    }
  }

  private UUID publishRequest(UUID consortiumId, PublicationRequest publicationRequest) {
    if (CollectionUtils.isNotEmpty(publicationRequest.getTenants())) {
      return publicationService.publishRequest(consortiumId, publicationRequest).getId();
    }
    log.info("publishRequest:: Tenant list of publishing for http method: {} is empty", publicationRequest.getMethod());
    return null;
  }

  private void checkEqualsOfPayloadIdWithSettingId(SharingSettingRequest sharingSettingRequest) {
    String sharingSettingId = String.valueOf(sharingSettingRequest.getSettingId());
    String payloadId = getPayloadId(sharingSettingRequest.getPayload());
    if (ObjectUtils.notEqual(sharingSettingId, payloadId)) {
      throw new IllegalArgumentException("Mismatch id in payload with settingId");
    }
  }

  private String getPayloadId(Object payload) {
    JsonNode payloadNode = objectMapper.convertValue(payload, JsonNode.class);
    return payloadNode.get("id").asText();
  }

  private PublicationRequest createPublicationRequestForSetting(SharingSettingRequest sharingSettingRequest, String httpMethod) {
    PublicationRequest publicationRequest = new PublicationRequest();
    publicationRequest.setMethod(httpMethod);
    String url = sharingSettingRequest.getUrl();
    if (httpMethod.equals(HttpMethod.PUT.toString()) || httpMethod.equals(HttpMethod.DELETE.toString())) {
      url += "/" + sharingSettingRequest.getSettingId();
    }
    publicationRequest.setUrl(url);
    publicationRequest.setPayload(sharingSettingRequest.getPayload());
    publicationRequest.setTenants(new HashSet<>());
    return publicationRequest;
  }

  private SharingSettingEntity createSharingSettingEntityFromRequest(SharingSettingRequest sharingSettingRequest, String tenantId) {
    SharingSettingEntity sharingSettingEntity = new SharingSettingEntity();
    sharingSettingEntity.setId(UUID.randomUUID());
    sharingSettingEntity.setSettingId(sharingSettingRequest.getSettingId());
    sharingSettingEntity.setTenantId(tenantId);
    return sharingSettingEntity;
  }
}
