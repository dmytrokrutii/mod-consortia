package org.folio.consortia.service.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.consortia.config.kafka.KafkaService;
import org.folio.consortia.domain.dto.PrimaryAffiliationEvent;
import org.folio.consortia.domain.dto.UserEvent;
import org.folio.consortia.service.TenantService;
import org.folio.consortia.service.UserAffiliationService;
import org.folio.consortia.service.UserTenantService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Log4j2
@AllArgsConstructor
public class UserAffiliationServiceImpl implements UserAffiliationService {

  private final UserTenantService userTenantService;
  private final TenantService tenantService;
  private final KafkaService kafkaService;
  private static final ObjectMapper OBJECT_MAPPER;

  static {
    OBJECT_MAPPER = new ObjectMapper()
      .findAndRegisterModules()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  }

  @Override
  @SneakyThrows
  public void createPrimaryUserAffiliation(String eventPayload) {
    try {
      var userEvent = OBJECT_MAPPER.readValue(eventPayload, UserEvent.class);
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
        userTenantService.createPrimaryUserTenantAffiliation(consortiaTenant.getConsortiumId(), consortiaTenant, userEvent);
      }

      PrimaryAffiliationEvent affiliationEvent = createPrimaryAffiliationEvent(userEvent);
      String data = OBJECT_MAPPER.writeValueAsString(affiliationEvent);

      kafkaService.send(KafkaService.Topic.CONSORTIUM_PRIMARY_AFFILIATION_CREATED, consortiaTenant.getConsortiumId().toString(), data);
      log.info("Primary affiliation has been set for the user: {}", userEvent.getUserDto().getId());
    } catch (Exception e) {
      log.error("Exception occurred while creating primary affiliation", e);
    }
  }

  @Override
  @SneakyThrows
  public void deletePrimaryUserAffiliation(String eventPayload) {
    try {
      var userEvent = OBJECT_MAPPER.readValue(eventPayload, UserEvent.class);
      log.info("Received event for deleting primary affiliation for user: {} and tenant: {}", userEvent.getUserDto().getId(), userEvent.getTenantId());

      var consortiaTenant = tenantService.getByTenantId(userEvent.getTenantId());
      if (consortiaTenant == null) {
        log.warn("Tenant {} not exists in consortia", userEvent.getTenantId());
        return;
      }

      userTenantService.deletePrimaryUserTenantAffiliation(UUID.fromString(userEvent.getUserDto().getId()));
      PrimaryAffiliationEvent affiliationEvent = createPrimaryAffiliationEvent(userEvent);
      String data = OBJECT_MAPPER.writeValueAsString(affiliationEvent);

      kafkaService.send(KafkaService.Topic.CONSORTIUM_PRIMARY_AFFILIATION_DELETED, consortiaTenant.getConsortiumId().toString(), data);
      log.info("Primary affiliation has been deleted for the user: {}", userEvent.getUserDto().getId());
    } catch (Exception e) {
      log.error("Exception occurred while deleting primary affiliation", e);
    }
  }

  private PrimaryAffiliationEvent createPrimaryAffiliationEvent(UserEvent userEvent) {
    PrimaryAffiliationEvent event = new PrimaryAffiliationEvent();
    event.setId(userEvent.getId());
    event.setUserId(UUID.fromString(userEvent.getUserDto().getId()));
    if (StringUtils.isNotBlank(userEvent.getUserDto().getUsername())) { // for delete event username will be empty
      event.setUserName(userEvent.getUserDto().getUsername());
    }
    event.setTenantId(userEvent.getTenantId());
    return event;
  }
}
