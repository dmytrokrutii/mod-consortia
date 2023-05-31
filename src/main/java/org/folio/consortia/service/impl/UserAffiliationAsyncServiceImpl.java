package org.folio.consortia.service.impl;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.folio.consortia.config.kafka.KafkaService;
import org.folio.consortia.domain.dto.PrimaryAffiliationEvent;
import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.domain.dto.User;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.repository.UserTenantRepository;
import org.folio.consortia.service.UserAffiliationAsyncService;
import org.folio.consortia.service.UserService;
import org.folio.consortia.service.UserTenantService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@AllArgsConstructor
public class UserAffiliationAsyncServiceImpl implements UserAffiliationAsyncService {

  private final UserTenantService userTenantService;
  private final KafkaService kafkaService;
  private final UserService userService;
  private final UserTenantRepository userTenantRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public CompletableFuture<Void> createPrimaryUserAffiliationsAsync(UUID consortiumId, TenantEntity consortiaTenant,
    Tenant tenantDto) {
    return CompletableFuture.runAsync(() -> {
        log.info("Start creating user primary affiliation for tenant {}", tenantDto.getId());
        var users = userService.getUsersByQuery(StringUtils.EMPTY, 0, Integer.MAX_VALUE);
        log.info("{} tenant users found", users.size());
        IntStream.range(0, users.size())
          .forEach(idx -> {
            var user = users.get(idx);
            log.info("Processing users: {} of {}", idx + 1, users.size());
            var consortiaUserTenant = userTenantRepository.findByUserIdAndTenantId(UUID.fromString(user.getId()), tenantDto.getId())
              .orElse(null);
            if (consortiaUserTenant != null && consortiaUserTenant.getIsPrimary()) {
              log.info("Primary affiliation already exists for tenant/user: {}/{}", tenantDto.getId(), user.getUsername());
            } else {
              userTenantService.createPrimaryUserTenantAffiliation(consortiumId, consortiaTenant, user.getId(), user.getUsername());
              sendCreatePrimaryAffiliationEvent(consortiaTenant, tenantDto, user);
            }
          });
      })
      .thenAccept(v -> log.info("Successfully created primary affiliations for tenant {}", tenantDto.getId()))
      .exceptionally(t -> {
        log.error("Failed to create primary affiliations for new tenant", t);
        return null;
      });
  }

  @SneakyThrows
  private void sendCreatePrimaryAffiliationEvent(TenantEntity consortiaTenant, Tenant tenantDto, User user) {
    PrimaryAffiliationEvent affiliationEvent = createPrimaryAffiliationEvent(user, tenantDto);
    String data = objectMapper.writeValueAsString(affiliationEvent);
    kafkaService.send(KafkaService.Topic.CONSORTIUM_PRIMARY_AFFILIATION_CREATED, consortiaTenant.getConsortiumId().toString(), data);
  }

  private PrimaryAffiliationEvent createPrimaryAffiliationEvent(User user, Tenant tenantDto) {
    PrimaryAffiliationEvent event = new PrimaryAffiliationEvent();
    event.setId(UUID.randomUUID());
    event.setUserId(UUID.fromString(user.getId()));
    if (StringUtils.isNotBlank(user.getUsername())) { // for delete event username will be empty
      event.setUsername(user.getUsername());
    }
    event.setTenantId(tenantDto.getId());
    return event;
  }
}
