package org.folio.consortia.messaging.listener;

import static org.folio.consortia.support.BaseTest.TENANT;
import static org.folio.consortia.utils.InputOutputTestUtils.getMockData;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.folio.consortia.service.ConsortiaConfigurationService;
import org.folio.consortia.service.UserAffiliationService;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.messaging.MessageHeaders;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class ConsortiaUserEventListenerTest {

  private static final String USER_CREATED_EVENT_SAMPLE = getMockData("mockdata/kafka/create_primary_affiliation_request.json");
  private static final String USER_DELETED_EVENT_SAMPLE = getMockData("mockdata/kafka/delete_primary_affiliation_request.json");

  @InjectMocks
  private ConsortiaUserEventListener eventListener;
  @Mock
  private UserAffiliationService userAffiliationService;
  @Mock
  private ConsortiaConfigurationService configurationService;

  @Test
  void shouldCreatePrimaryAffiliationWhenConfigurationExists() {
    when(configurationService.getCentralTenantId(TENANT)).thenReturn(TENANT);
    eventListener.handleUserCreating(USER_CREATED_EVENT_SAMPLE, getMessageHeaders());
    verify(userAffiliationService).createPrimaryUserAffiliation(anyString());
  }

  @Test
  void shouldDeletePrimaryAffiliationWhenConfigurationExists() {
    when(configurationService.getCentralTenantId(TENANT)).thenReturn(TENANT);
    eventListener.handleUserDeleting(USER_DELETED_EVENT_SAMPLE, getMessageHeaders());
    verify(userAffiliationService).deletePrimaryUserAffiliation(anyString());
  }

  @Test
  void shouldThrowErrorForUserCreatedWhenBusinessExceptionThrown() {
    when(configurationService.getCentralTenantId(TENANT)).thenThrow(new RuntimeException("Operation failed"));
    assertThrows(java.lang.RuntimeException.class, () -> eventListener.handleUserCreating(USER_CREATED_EVENT_SAMPLE, getMessageHeaders()));
  }

  @Test
  void shouldThrowErrorForUserDeletedWhenBusinessExceptionThrown() {
    when(configurationService.getCentralTenantId(TENANT)).thenThrow(new RuntimeException("Operation failed"));
    assertThrows(java.lang.RuntimeException.class, () -> eventListener.handleUserDeleting(USER_DELETED_EVENT_SAMPLE, getMessageHeaders()));
  }

  @Test
  void shouldNotThrowErrorForUserCreatedWhenCouldNotGetCentralTenantId() {
    // in case when we have consortium and standalone tenants in the same cluster - we should skip processing of event from standalone tenant
    when(configurationService.getCentralTenantId(TENANT))
      .thenThrow(new BadSqlGrammarException("table 'consortia_configuration' not found", "", new SQLException()));
    eventListener.handleUserCreating(USER_CREATED_EVENT_SAMPLE, getMessageHeaders());
    verifyNoInteractions(userAffiliationService);
  }

  @Test
  void shouldNotThrowErrorForUserDeletedWhenCouldNotGetCentralTenantId() {
    // in case when we have consortium and standalone tenants in the same cluster - we should skip processing of event from standalone tenant
    when(configurationService.getCentralTenantId(TENANT))
      .thenThrow(new BadSqlGrammarException("table 'consortia_configuration' not found", "", new SQLException()));
    eventListener.handleUserDeleting(USER_DELETED_EVENT_SAMPLE, getMessageHeaders());
    verifyNoInteractions(userAffiliationService);
  }

  private MessageHeaders getMessageHeaders() {
    Map<String, Object> header = new HashMap<>();
    header.put(XOkapiHeaders.TENANT, TENANT.getBytes());

    return new MessageHeaders(header);
  }
}
