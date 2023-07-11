package org.folio.consortia.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.consortia.client.SyncPrimaryAffiliationClient;
import org.folio.consortia.config.kafka.KafkaService;
import org.folio.consortia.domain.dto.PrimaryAffiliationEvent;
import org.folio.consortia.domain.dto.SyncPrimaryAffiliationBody;
import org.folio.consortia.domain.dto.SyncUser;
import org.folio.consortia.domain.dto.User;
import org.folio.consortia.domain.dto.UserTenant;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.domain.entity.UserTenantEntity;
import org.folio.consortia.repository.UserTenantRepository;
import org.folio.consortia.service.SyncPrimaryAffiliationService;
import org.folio.consortia.service.TenantService;
import org.folio.consortia.service.UserService;
import org.folio.consortia.service.UserTenantService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class SyncPrimaryAffiliationServiceImpl implements SyncPrimaryAffiliationService {
  private final UserService userService;
  private final UserTenantService userTenantService;
  private final TenantService tenantService;
  private final UserTenantRepository userTenantRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final KafkaService kafkaService;
  private final SyncPrimaryAffiliationClient syncPrimaryAffiliationClient;

  @Override
  public void syncPrimaryAffiliations(UUID consortiumId, String tenantId, String centralTenantId) {
    log.info("Start syncing user primary affiliations for tenant {}", tenantId);
    List<User> users = new ArrayList<>();
    try {
      users = userService.getUsersByQuery("cql.allRecords=1", 0, Integer.MAX_VALUE);
    } catch (Exception e) {
      log.error("syncPrimaryAffiliations:: failed to retrieve '{}' users", tenantId, e);
    }
    if (CollectionUtils.isNotEmpty(users)) {
      SyncPrimaryAffiliationBody spab = buildSyncPrimaryAffiliationBody(tenantId, users);
      syncPrimaryAffiliationClient.savePrimaryAffiliations(spab, consortiumId.toString(), tenantId, centralTenantId);
    }
  }

  private SyncPrimaryAffiliationBody buildSyncPrimaryAffiliationBody(String tenantId, List<User> users) {
    var syncUsers = users.stream()
      .map(user -> new SyncUser().id(user.getId())
        .username(user.getUsername()))
      .toList();
    return new SyncPrimaryAffiliationBody().tenantId(tenantId)
      .users(syncUsers);
  }

  @Override
  public void createPrimaryUserAffiliations(UUID consortiumId, String centralTenantId,
    SyncPrimaryAffiliationBody syncPrimaryAffiliationBody) {
    log.info("Start creating user primary affiliation for tenant {}", syncPrimaryAffiliationBody.getTenantId());
    var tenantId = syncPrimaryAffiliationBody.getTenantId();
    var userList = syncPrimaryAffiliationBody.getUsers();

    try {
      TenantEntity tenantEntity = tenantService.getByTenantId(tenantId);
      IntStream.range(0, userList.size()).sequential().forEach(idx -> {
        var user = userList.get(idx);
        log.info("Processing users: {} of {}", idx + 1, userList.size());
        Page<UserTenantEntity> userTenantPage = userTenantRepository.findByUserId(UUID.fromString(user.getId()), PageRequest.of(0, 1));

        if (userTenantPage.getTotalElements() > 0) {
          log.info("Primary affiliation already exists for tenant/user: {}/{}", tenantId, user.getUsername());
        } else {
          userTenantService.createPrimaryUserTenantAffiliation(consortiumId, tenantEntity, user.getId(), user.getUsername());
          if (ObjectUtils.notEqual(centralTenantId, tenantEntity.getId())) {
            userTenantService.save(consortiumId, createUserTenant(centralTenantId, user), true);
          }
          sendCreatePrimaryAffiliationEvent(tenantEntity, user);
        }

      });
      log.info("Successfully created primary affiliations for tenant {}", tenantId);
    } catch (Exception e) {
      log.error("Failed to create primary affiliations for tenant {}", tenantId, e);
    }
  }

  @SneakyThrows
  private void sendCreatePrimaryAffiliationEvent(TenantEntity consortiaTenant, SyncUser user) {
    PrimaryAffiliationEvent affiliationEvent = createPrimaryAffiliationEvent(user, consortiaTenant.getId());
    String data = objectMapper.writeValueAsString(affiliationEvent);
    kafkaService.send(KafkaService.Topic.CONSORTIUM_PRIMARY_AFFILIATION_CREATED, consortiaTenant.getConsortiumId().toString(), data);
  }

  private UserTenant createUserTenant(String tenantId, SyncUser user) {
    UserTenant userTenant = new UserTenant();
    userTenant.setTenantId(tenantId);
    userTenant.setUserId(UUID.fromString(user.getId()));
    userTenant.setUsername(user.getUsername());
    return userTenant;
  }

  private PrimaryAffiliationEvent createPrimaryAffiliationEvent(SyncUser user, String tenantId) {
    PrimaryAffiliationEvent event = new PrimaryAffiliationEvent();
    event.setId(UUID.randomUUID());
    event.setUserId(UUID.fromString(user.getId()));
    event.setUsername(user.getUsername());
    event.setTenantId(tenantId);
    return event;
  }
}
