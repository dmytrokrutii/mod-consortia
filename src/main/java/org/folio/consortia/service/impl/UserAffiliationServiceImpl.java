package org.folio.consortia.service.impl;

import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.consortia.config.kafka.KafkaService;
import org.folio.consortia.domain.dto.PrimaryAffiliationEvent;
import org.folio.consortia.domain.dto.User;
import org.folio.consortia.domain.dto.UserEvent;
import org.folio.consortia.domain.dto.UserTenant;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.domain.entity.UserTenantEntity;
import org.folio.consortia.service.TenantService;
import org.folio.consortia.service.UserAffiliationService;
import org.folio.consortia.service.UserTenantService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@AllArgsConstructor
public class UserAffiliationServiceImpl implements UserAffiliationService {
  private static final String EVENT_PAYLOAD_COULD_NOT_BE_PARSED = "Skipping user affiliation event because input payload: {} could not be parsed";
  private static final String TENANT_NOT_EXISTS_IN_CONSORTIA = "Tenant {} not exists in consortia";

  private final UserTenantService userTenantService;
  private final TenantService tenantService;
  private final KafkaService kafkaService;
  private final FolioExecutionContext folioExecutionContext;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  @Transactional
  public void createPrimaryUserAffiliation(String eventPayload) {
    String centralTenantId = folioExecutionContext.getTenantId();
    var pair = getDataFromPayload(eventPayload);
    if (Objects.isNull(pair)) {
      return;
    }
    var userEvent = pair.getLeft();
    var consortiaTenant = pair.getRight();

    try {
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

      PrimaryAffiliationEvent affiliationEvent = createPrimaryAffiliationEvent(userEvent, centralTenantId);
      String data = objectMapper.writeValueAsString(affiliationEvent);

      kafkaService.send(KafkaService.Topic.CONSORTIUM_PRIMARY_AFFILIATION_CREATED, consortiaTenant.getConsortiumId().toString(), data);
      log.info("Primary affiliation has been set for the user: {}", userEvent.getUserDto().getId());
    } catch (Exception e) {
      log.error("Exception occurred while creating primary affiliation for userId: {}, tenant: {} and error message: {}",
        userEvent.getUserDto().getId(), userEvent.getTenantId(), e.getMessage(), e);
    }
  }

  @Override
  @Transactional
  public void updatePrimaryUserAffiliation(String eventPayload) {
    String centralTenantId = folioExecutionContext.getTenantId();
    var pair = getDataFromPayload(eventPayload);
    if (Objects.isNull(pair)) {
      return;
    }
    var userEvent = pair.getLeft();
    var consortiaTenant = pair.getRight();

    try {
      UUID userId = getUserId(userEvent);
      String newUsername = userEvent.getUserDto().getUsername();

      UserTenantEntity userTenant = userTenantService.getByUserIdAndTenantId(userId, userEvent.getTenantId());
      boolean isUsernameChanged = ObjectUtils.notEqual(userTenant.getUsername(), newUsername);

      if (isUsernameChanged) {
        userTenantService.updateUsernameInPrimaryUserTenantAffiliation(userId, newUsername, userEvent.getTenantId());
        log.info("Username in primary affiliation has been updated for the user: {}", userEvent.getUserDto().getId());
      }

      PrimaryAffiliationEvent affiliationEvent = createPrimaryAffiliationEvent(userEvent, centralTenantId);
      String data = objectMapper.writeValueAsString(affiliationEvent);

      kafkaService.send(KafkaService.Topic.CONSORTIUM_PRIMARY_AFFILIATION_UPDATED, consortiaTenant.getConsortiumId().toString(), data);
    } catch (Exception e) {
      log.error("Exception occurred while updating primary affiliation for userId: {}, tenant: {} and error message: {}",
        userEvent.getUserDto().getId(), userEvent.getTenantId(), e.getMessage(), e);
    }
  }

  @Override
  @Transactional
  public void deletePrimaryUserAffiliation(String eventPayload) {
    String centralTenantId = folioExecutionContext.getTenantId();
    var pair = getDataFromPayload(eventPayload);
    if (Objects.isNull(pair)) {
      return;
    }
    var userEvent = pair.getLeft();
    var consortiaTenant = pair.getRight();

    try {
      userTenantService.deletePrimaryUserTenantAffiliation(getUserId(userEvent));
      userTenantService.deleteShadowUsers(getUserId(userEvent));

      PrimaryAffiliationEvent affiliationEvent = createPrimaryAffiliationEvent(userEvent, centralTenantId);
      String data = objectMapper.writeValueAsString(affiliationEvent);

      kafkaService.send(KafkaService.Topic.CONSORTIUM_PRIMARY_AFFILIATION_DELETED, consortiaTenant.getConsortiumId().toString(), data);
      log.info("Primary affiliation has been deleted for the user: {}", userEvent.getUserDto().getId());
    } catch (Exception e) {
      log.error("Exception occurred while deleting primary affiliation for userId: {}, tenant: {} and error message: {}",
        userEvent.getUserDto().getId(), userEvent.getTenantId(), e.getMessage(), e);
    }
  }

  private Pair<UserEvent, TenantEntity> getDataFromPayload(String eventPayload) {
    UserEvent userEvent = parseUserEvent(eventPayload);
    if (Objects.isNull(userEvent)) {
      log.warn(EVENT_PAYLOAD_COULD_NOT_BE_PARSED, eventPayload);
      return null;
    }

    var consortiaTenant = tenantService.getByTenantId(userEvent.getTenantId());
    if (Objects.isNull(consortiaTenant)) {
      log.warn(TENANT_NOT_EXISTS_IN_CONSORTIA, userEvent.getTenantId());
      return null;
    }
    return Pair.of(userEvent, consortiaTenant);
  }

  private UserEvent parseUserEvent(String eventPayload) {
    try {
      var userEvent = objectMapper.readValue(eventPayload, UserEvent.class);
      log.info("Received {} event for userId: {} and tenant: {}",
        userEvent.getAction(), userEvent.getUserDto().getId(), userEvent.getTenantId());
      return userEvent;
    } catch (Exception e) {
      log.error("Could not parse input payload for processing user event", e);
      return null;
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

  private PrimaryAffiliationEvent createPrimaryAffiliationEvent(UserEvent userEvent, String centralTenantId) {
    PrimaryAffiliationEvent event = new PrimaryAffiliationEvent();
    event.setId(userEvent.getId());
    event.setUserId(UUID.fromString(userEvent.getUserDto().getId()));

    User userDto = userEvent.getUserDto();
    if (StringUtils.isNotBlank(userDto.getUsername())) { // for delete event username will be empty
      event.setUsername(userEvent.getUserDto().getUsername());
      if (ObjectUtils.isNotEmpty(userDto.getPersonal())) {
        event.setEmail(userDto.getPersonal().getEmail());
        event.setPhoneNumber(userDto.getPersonal().getPhone());
        event.setMobilePhoneNumber(userDto.getPersonal().getMobilePhone());
      }
    }
    event.setTenantId(userEvent.getTenantId());
    event.setCentralTenantId(centralTenantId);
    return event;
  }
}
