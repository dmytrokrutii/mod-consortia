package org.folio.consortia.messaging.listener;

import static org.folio.consortia.utils.TenantContextUtils.getFolioExecutionContextCreatePrimaryAffiliationEvent;
import static org.folio.consortia.utils.TenantContextUtils.getFolioExecutionContextDeletePrimaryAffiliationEvent;
import static org.folio.consortia.utils.TenantContextUtils.runInFolioContext;

import org.folio.consortia.service.UserAffiliationService;
import org.folio.spring.FolioModuleMetadata;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class ConsortiaEventListener {

  public static final String USER_CREATED_LISTENER_ID = "user-created-listener-id";
  public static final String USER_DELETED_LISTENER_ID = "user-deleted-listener-id";
  private final UserAffiliationService userAffiliationService;
  private final FolioModuleMetadata moduleMetadata;

  @KafkaListener(
    id = USER_CREATED_LISTENER_ID,
    topicPattern = "#{folioKafkaProperties.listener['user-created'].topicPattern}",
    concurrency = "#{folioKafkaProperties.listener['user-created'].concurrency}",
    containerFactory = "kafkaListenerContainerFactory")
  public void userCreatedListener(String data, MessageHeaders messageHeaders) {
    runInFolioContext(getFolioExecutionContextCreatePrimaryAffiliationEvent(messageHeaders, moduleMetadata),
      () -> userAffiliationService.createPrimaryUserAffiliation(data));
  }

  @KafkaListener(
    id = USER_DELETED_LISTENER_ID,
    topicPattern = "#{folioKafkaProperties.listener['user-deleted'].topicPattern}",
    concurrency = "#{folioKafkaProperties.listener['user-deleted'].concurrency}",
    containerFactory = "kafkaListenerContainerFactory")
  public void userDeletedListener(String data, MessageHeaders messageHeaders) {
    runInFolioContext(getFolioExecutionContextDeletePrimaryAffiliationEvent(messageHeaders, moduleMetadata),
      () -> userAffiliationService.deletePrimaryUserAffiliation(data));
  }

}
