package org.folio.consortia.config.kafka;

import static org.folio.consortia.messaging.listener.ConsortiaEventListener.USER_CREATED_LISTENER_ID;

import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.NewTopic;
import org.folio.consortia.config.kafka.properties.FolioKafkaProperties;
import org.folio.consortia.messaging.domain.ConsortiaInputEventType;
import org.folio.consortia.messaging.domain.ConsortiaOutputEventType;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class KafkaService {

  public static final String EVENT_LISTENER_ID = "mod-consortia-events-listener";

  private final KafkaAdmin kafkaAdmin;
  private final BeanFactory beanFactory;
  private final FolioExecutionContext folioExecutionContext;
  private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
  private final FolioKafkaProperties folioKafkaProperties;
  private final Environment springEnvironment;
  private final String kafkaEnvId;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  @RequiredArgsConstructor
  @AllArgsConstructor
  @Getter
  public enum Topic {
    USER_CREATED("USER_CREATED"),
    USER_DELETED("USER_DELETED"),
    CONSORTIUM_PRIMARY_AFFILIATION_CREATED("Default", "CONSORTIUM_PRIMARY_AFFILIATION_CREATED"),
    CONSORTIUM_PRIMARY_AFFILIATION_DELETED("Default", "CONSORTIUM_PRIMARY_AFFILIATION_DELETED");
    private String nameSpace;
    private final String topicName;
  }

  public void createKafkaTopics() {
    if (folioExecutionContext == null) {
      throw new IllegalStateException("Could be executed only in Folio-request scope");
    }
    var tenantId = folioExecutionContext.getTenantId();
    var topicList = tenantSpecificTopics(tenantId);

    log.info("Creating topics for kafka [topics: {}]", topicList);
    var configurableBeanFactory = (ConfigurableBeanFactory) beanFactory;
    topicList.forEach(newTopic -> {
      var beanName = newTopic.name() + ".topic";
      if (!configurableBeanFactory.containsBean(beanName)) {
        configurableBeanFactory.registerSingleton(beanName, newTopic);
      }
    });
    kafkaAdmin.initialize();
    restartEventListeners();
  }

  /**
   * Restarts kafka event listeners in mod-consortia application.
   */
  public void restartEventListeners() {
    log.info("Restarting kafka consumer to start listening created topics [id: {}]", USER_CREATED_LISTENER_ID);
    var listenerContainer = kafkaListenerEndpointRegistry.getListenerContainer(USER_CREATED_LISTENER_ID);
    Assert.notNull(listenerContainer, "Listener container not found");
    listenerContainer.stop();
    listenerContainer.start();
  }

  private List<NewTopic> tenantSpecificTopics(String tenant) {
    var eventsNameStreamBuilder = Stream.<Enum<?>>builder();
    for (ConsortiaInputEventType consEventType : ConsortiaInputEventType.values()) {
      eventsNameStreamBuilder.add(consEventType);
    }
    eventsNameStreamBuilder.add(ConsortiaOutputEventType.CONSORTIUM_PRIMARY_AFFILIATION_CREATED);
    return eventsNameStreamBuilder.build()
      .map(Enum::name)
      .map(topic -> getTenantTopicName(topic, tenant))
      .map(this::toKafkaTopic)
      .toList();
  }

  private NewTopic toKafkaTopic(String topic) {
    return TopicBuilder.name(topic)
      .replicas(folioKafkaProperties.getReplicationFactor())
      .partitions(folioKafkaProperties.getNumberOfPartitions())
      .build();
  }

  /**
   * Returns topic name in the format - `{env}.{tenant}.topicName`
   *
   * @param topicName initial topic name as {@link String}
   * @param tenantId tenant id as {@link String}
   * @return topic name as {@link String} object
   */
  private String getTenantTopicName(String topicName, String tenantId) {
    return String.format("%s.Default.%s.%s", kafkaEnvId, tenantId, topicName);
  }

  public void send(Topic topic, String key, Object data) {
    log.info("Sending {}.", data);
    String tenant = folioExecutionContext.getTenantId();
    if (StringUtils.isBlank(tenant)) {
      throw new IllegalStateException("Can't send to Kafka because tenant is blank");
    }
    kafkaTemplate.send(getTenantTopicName(topic.getTopicName(), tenant), key, data);
    log.info("Sent {}.", data);
  }
}
