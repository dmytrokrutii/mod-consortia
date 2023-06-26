package org.folio.consortia.messaging.listener;

import static org.folio.consortia.utils.TenantContextUtils.createFolioExecutionContext;
import static org.folio.consortia.utils.TenantContextUtils.getHeaderValue;
import static org.folio.consortia.utils.TenantContextUtils.runInFolioContext;

import org.folio.consortia.service.ConsortiaConfigurationService;
import org.folio.consortia.service.UserAffiliationService;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class ConsortiaUserEventListener {

  public static final String USER_CREATED_LISTENER_ID = "user-created-listener-id";
  public static final String USER_DELETED_LISTENER_ID = "user-deleted-listener-id";
  private final UserAffiliationService userAffiliationService;
  private final ConsortiaConfigurationService configurationService;
  private final FolioModuleMetadata folioMetadata;

  @KafkaListener(
    id = USER_CREATED_LISTENER_ID,
    topicPattern = "#{folioKafkaProperties.listener['user-created'].topicPattern}",
    concurrency = "#{folioKafkaProperties.listener['user-created'].concurrency}",
    containerFactory = "kafkaListenerContainerFactory")
  public void handleUserCreating(String data, MessageHeaders messageHeaders) {
    // to create affiliation in central tenant schema
    String centralTenantId = getCentralTenantByIdByHeader(messageHeaders);
    if (StringUtils.isNotBlank(centralTenantId)) {
      runInFolioContext(createFolioExecutionContext(messageHeaders, folioMetadata, centralTenantId), () ->
        userAffiliationService.createPrimaryUserAffiliation(data));
    }
  }

  @KafkaListener(
    id = USER_DELETED_LISTENER_ID,
    topicPattern = "#{folioKafkaProperties.listener['user-deleted'].topicPattern}",
    concurrency = "#{folioKafkaProperties.listener['user-deleted'].concurrency}",
    containerFactory = "kafkaListenerContainerFactory")
  public void handleUserDeleting(String data, MessageHeaders messageHeaders) {
    // to delete affiliation from central tenant schema
    String centralTenantId = getCentralTenantByIdByHeader(messageHeaders);
    if (StringUtils.isNotBlank(centralTenantId)) {
      runInFolioContext(createFolioExecutionContext(messageHeaders, folioMetadata, centralTenantId), () ->
        userAffiliationService.deletePrimaryUserAffiliation(data));
    }
  }

  public String getCentralTenantByIdByHeader(MessageHeaders messageHeaders) {
    String requestedTenantId = getTenantIdFromKafkaHeaders(messageHeaders);
    // getting central tenant for this requested tenant from get central in its own schema
    try (var context = new FolioExecutionContextSetter(createFolioExecutionContext(
      messageHeaders, folioMetadata, requestedTenantId))) {
      return configurationService.getCentralTenantId(requestedTenantId);
    } catch (InvalidDataAccessResourceUsageException e) {
      log.info("Table consortia_configuration is not exists, because tenant: {} is not in consortium, DB message: {}, skipping...",
        requestedTenantId, e.getMessage());
    }

    return null;
  }

  private String getTenantIdFromKafkaHeaders(MessageHeaders messageHeaders) {
    return getHeaderValue(messageHeaders, XOkapiHeaders.TENANT, null).get(0);
  }

}
