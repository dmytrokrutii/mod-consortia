package org.folio.consortia.service.impl;

import static org.folio.consortia.utils.TenantContextUtils.prepareContextForTenant;

import java.util.UUID;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.consortia.config.kafka.KafkaService;
import org.folio.consortia.domain.dto.PrimaryAffiliationEvent;
import org.folio.consortia.domain.dto.UserEvent;
import org.folio.consortia.domain.dto.UserTenant;
import org.folio.consortia.service.TenantService;
import org.folio.consortia.service.UserAffiliationService;
import org.folio.consortia.service.UserTenantService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@AllArgsConstructor
public class UserAffiliationServiceImpl implements UserAffiliationService {

  private final UserTenantService userTenantService;
  private final TenantService tenantService;
  private final KafkaService kafkaService;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  @SneakyThrows
  @Transactional
  public void createPrimaryUserAffiliation(String eventPayload) {
    FolioExecutionContext currentContext = (FolioExecutionContext) folioExecutionContext.getInstance();
    String centralTenantId = folioExecutionContext.getTenantId();
    try {
      var userEvent = objectMapper.readValue(eventPayload, UserEvent.class);
      log.info("Received event for creating primary affiliation for user: {} and tenant: {}", userEvent.getUserDto().getId(), userEvent.getTenantId());

      var consortiaTenant = tenantService.getByTenantId(userEvent.getTenantId());
      if (consortiaTenant == null) {
        log.warn("Tenant {} not exists in consortia", userEvent.getTenantId());
        return;
      }

      boolean isPrimaryAffiliationExists = userTenantService
        .checkUserIfHasPrimaryAffiliationByUserId(consortiaTenant.getConsortiumId(), userEvent.getUserDto().getId());
      if (isPrimaryAffiliationExists) {
        log.warn("Primary affiliation already exists for tenant/user: {}/{}", userEvent.getTenantId(), userEvent.getUserDto().getUsername());
        return;
      } else {
        userTenantService.createPrimaryUserTenantAffiliation(consortiaTenant.getConsortiumId(), consortiaTenant, userEvent.getUserDto().getId(), userEvent.getUserDto().getUsername());
        if (ObjectUtils.notEqual(centralTenantId, consortiaTenant.getId())) {
          userTenantService.save(consortiaTenant.getConsortiumId(), createUserTenant(centralTenantId, userEvent), false);
        }
      }

      PrimaryAffiliationEvent affiliationEvent = createPrimaryAffiliationEvent(userEvent);
      String data = objectMapper.writeValueAsString(affiliationEvent);

      // context is changed in save() method and context is empty after save() method, so we need to set context again.
      try (var context = new FolioExecutionContextSetter(prepareContextForTenant(centralTenantId, folioModuleMetadata, currentContext))) {
        kafkaService.send(KafkaService.Topic.CONSORTIUM_PRIMARY_AFFILIATION_CREATED, consortiaTenant.getConsortiumId().toString(), data);
      }
      log.info("Primary affiliation has been set for the user: {}", userEvent.getUserDto().getId());
    } catch (Exception e) {
      log.error("Exception occurred while creating primary affiliation", e);
    }
  }

  @Override
  @SneakyThrows
  @Transactional
  public void deletePrimaryUserAffiliation(String eventPayload) {
    FolioExecutionContext currentContext = (FolioExecutionContext) folioExecutionContext.getInstance();
    String centralTenantId = folioExecutionContext.getTenantId();
    try {
      var userEvent = objectMapper.readValue(eventPayload, UserEvent.class);
      log.info("Received event for deleting primary affiliation for user: {} and tenant: {}", userEvent.getUserDto().getId(), userEvent.getTenantId());

      var consortiaTenant = tenantService.getByTenantId(userEvent.getTenantId());
      if (consortiaTenant == null) {
        log.warn("Tenant {} not exists in consortia", userEvent.getTenantId());
        return;
      }

      userTenantService.deletePrimaryUserTenantAffiliation(getUserId(userEvent));
      userTenantService.deleteShadowUsers(getUserId(userEvent));

      PrimaryAffiliationEvent affiliationEvent = createPrimaryAffiliationEvent(userEvent);
      String data = objectMapper.writeValueAsString(affiliationEvent);

      // context is changed in deleteShadowUsers() method and context is empty after deleteShadowUsers() method, so we need to set context again.
      try (var context = new FolioExecutionContextSetter(prepareContextForTenant(centralTenantId, folioModuleMetadata, currentContext))) {
        kafkaService.send(KafkaService.Topic.CONSORTIUM_PRIMARY_AFFILIATION_DELETED, consortiaTenant.getConsortiumId().toString(), data);
      }
      log.info("Primary affiliation has been deleted for the user: {}", userEvent.getUserDto().getId());
    } catch (Exception e) {
      log.error("Exception occurred while deleting primary affiliation", e);
    }
  }

  private UUID getUserId(UserEvent userEvent) {
    if (StringUtils.isBlank(userEvent.getUserDto().getId())) {
      throw new IllegalArgumentException("User id is empty");
    }
    return UUID.fromString(userEvent.getUserDto().getId());
  }

  private UserTenant createUserTenant(String tenantId, UserEvent userEvent) {
    UserTenant userTenant = new UserTenant();
    userTenant.setTenantId(tenantId);
    userTenant.setUserId(UUID.fromString(userEvent.getUserDto().getId()));
    userTenant.setUsername(userEvent.getUserDto().getUsername());
    return userTenant;
  }

  private PrimaryAffiliationEvent createPrimaryAffiliationEvent(UserEvent userEvent) {
    PrimaryAffiliationEvent event = new PrimaryAffiliationEvent();
    event.setId(userEvent.getId());
    event.setUserId(UUID.fromString(userEvent.getUserDto().getId()));
    if (StringUtils.isNotBlank(userEvent.getUserDto().getUsername())) { // for delete event username will be empty
      event.setUsername(userEvent.getUserDto().getUsername());
    }
    event.setTenantId(userEvent.getTenantId());
    return event;
  }
}
