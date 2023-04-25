package org.folio.consortia.service.impl;

import org.folio.consortia.config.kafka.KafkaService;
import org.folio.consortia.service.TenantService;
import org.folio.consortia.service.UserAffiliationService;
import org.folio.consortia.service.UserTenantService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
  private static final ObjectMapper OBJECT_MAPPER;

  static {
    OBJECT_MAPPER =
      new ObjectMapper()
        .findAndRegisterModules()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  }

  @Override
  @SneakyThrows
  public void createPrimaryUserAffiliation(String eventPayload) {
    try {
      var userEvent = OBJECT_MAPPER.readValue(eventPayload, org.folio.consortia.domain.dto.UserEvent.class);
      // check if tenant is part of consortia
      var consortiaTenant = tenantService.getByTenantId(userEvent.getTenantId());
      if (consortiaTenant == null) {
        log.warn("Tenant {} not exists in consortia", userEvent.getTenantId());
        return;
      }
      // check if tenant is part of consortia
      var consortiaUserTenant = userTenantService.getByUsernameAndTenantIdOrNull(consortiaTenant.getConsortiumId(),
        userEvent.getUserDto().getUsername(),
        userEvent.getTenantId());
      if (consortiaUserTenant != null && consortiaUserTenant.getIsPrimary()) {
        log.warn("Primary affiliation already exists for tenant/user: {}/{}", userEvent.getTenantId(), userEvent.getUserDto().getUsername());
        return;
      } else {
        userTenantService.createPrimaryUserTenantAffiliation(consortiaTenant.getConsortiumId(), consortiaTenant, userEvent);
      }

      kafkaService.send(KafkaService.Topic.CONSORTIUM_PRIMARY_AFFILIATION_CREATED, consortiaTenant.getConsortiumId().toString(), userEvent);
      log.info("Primary affiliation has been set for the user: {}", userEvent.getUserDto().getId());
    } catch (Exception e) {
      log.error("Exception occurred while creating primary affiliation", e);
    }
  }
}
