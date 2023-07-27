package org.folio.consortia.messaging.listener;

import static org.folio.consortia.support.BaseIT.TENANT;
import static org.folio.consortia.utils.InputOutputTestUtils.getMockDataAsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.folio.consortia.service.UserAffiliationService;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.messaging.MessageHeaders;

@SpringBootTest
class ConsortiaUserEventListenerTest {

  private static final String USER_CREATED_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/create_primary_affiliation_request.json");
  private static final String USER_UPDATED_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/update_primary_affiliation_request.json");
  private static final String USER_DELETED_EVENT_SAMPLE = getMockDataAsString("mockdata/kafka/delete_primary_affiliation_request.json");

  @InjectMocks
  private ConsortiaUserEventListener eventListener;
  @Mock
  private UserAffiliationService userAffiliationService;
  @Mock
  private EventListenerHelper eventListenerHelper;

  @Test
  void shouldCreatePrimaryAffiliationWhenConfigurationExists() {
    MessageHeaders messageHeaders = getMessageHeaders();
    when(eventListenerHelper.getCentralTenantByIdByHeader(messageHeaders)).thenReturn(TENANT);
    eventListener.handleUserCreating(USER_CREATED_EVENT_SAMPLE, messageHeaders);
    verify(userAffiliationService).createPrimaryUserAffiliation(anyString());
  }

  @Test
  void shouldUpdatePrimaryAffiliationWhenConfigurationExists() {
    MessageHeaders messageHeaders = getMessageHeaders();
    when(eventListenerHelper.getCentralTenantByIdByHeader(messageHeaders)).thenReturn(TENANT);
    eventListener.handleUserUpdating(USER_UPDATED_EVENT_SAMPLE, messageHeaders);
    verify(userAffiliationService).updatePrimaryUserAffiliation(anyString());
  }

  @Test
  void shouldDeletePrimaryAffiliationWhenConfigurationExists() {
    MessageHeaders messageHeaders = getMessageHeaders();
    when(eventListenerHelper.getCentralTenantByIdByHeader(messageHeaders)).thenReturn(TENANT);
    eventListener.handleUserDeleting(USER_DELETED_EVENT_SAMPLE, messageHeaders);
    verify(userAffiliationService).deletePrimaryUserAffiliation(anyString());
  }

  @Test
  void shouldThrowErrorForUserCreatedWhenBusinessExceptionThrown() {
    MessageHeaders messageHeaders = getMessageHeaders();
    when(eventListenerHelper.getCentralTenantByIdByHeader(messageHeaders)).thenThrow(new RuntimeException("Operation failed"));
    assertThrows(java.lang.RuntimeException.class,
      () -> eventListener.handleUserCreating(USER_CREATED_EVENT_SAMPLE, messageHeaders));
  }

  @Test
  void shouldThrowErrorForUserDeletedWhenBusinessExceptionThrown() {
    MessageHeaders messageHeaders = getMessageHeaders();
    when(eventListenerHelper.getCentralTenantByIdByHeader(messageHeaders)).thenThrow(new RuntimeException("Operation failed"));
    assertThrows(java.lang.RuntimeException.class,
      () -> eventListener.handleUserDeleting(USER_DELETED_EVENT_SAMPLE, messageHeaders));
  }

  @Test
  void shouldNotThrowErrorForUserCreatedWhenCouldNotGetCentralTenantId() {
    // in case when we have consortium and standalone tenants in the same cluster - we should skip processing of event from standalone tenant
    MessageHeaders messageHeaders = getMessageHeaders();
    when(eventListenerHelper.getCentralTenantByIdByHeader(messageHeaders)).
      thenThrow(new BadSqlGrammarException("table 'consortia_configuration' not found", "", new SQLException()));
    assertThrows(org.springframework.jdbc.BadSqlGrammarException.class,
      () -> eventListener.handleUserCreating(USER_CREATED_EVENT_SAMPLE, messageHeaders));
    verifyNoInteractions(userAffiliationService);
  }

  @Test
  void shouldNotThrowErrorForUserDeletedWhenCouldNotGetCentralTenantId() {
    // in case when we have consortium and standalone tenants in the same cluster - we should skip processing of event from standalone tenant
    MessageHeaders messageHeaders = getMessageHeaders();
    when(eventListenerHelper.getCentralTenantByIdByHeader(messageHeaders)).
      thenThrow(new BadSqlGrammarException("table 'consortia_configuration' not found", "", new SQLException()));
    assertThrows(org.springframework.jdbc.BadSqlGrammarException.class,
      () -> eventListener.handleUserDeleting(USER_DELETED_EVENT_SAMPLE, messageHeaders));
    verifyNoInteractions(userAffiliationService);
  }

  private MessageHeaders getMessageHeaders() {
    Map<String, Object> header = new HashMap<>();
    header.put(XOkapiHeaders.TENANT, TENANT.getBytes());
    return new MessageHeaders(header);
  }
}
