package org.folio.consortia.service;

import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.consortia.config.kafka.KafkaService;
import org.folio.consortia.repository.TenantRepository;
import org.folio.consortia.service.impl.UserAffiliationServiceImpl;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.folio.consortia.utils.EntityUtils.createTenantEntity;
import static org.folio.consortia.utils.InputOutputTestUtils.getMockData;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.TOKEN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


class UserAffiliationServiceTest {
  private static final String userCreatedEventSample = getMockData("mockdata/kafka/create_primary_affiliation_request.json");
  private static final String userDeletedEventSample = getMockData("mockdata/kafka/delete_primary_affiliation_request.json");
  @Mock
  private FolioModuleMetadata folioModuleMetadata;
  @InjectMocks
  UserAffiliationServiceImpl userAffiliationService;
  @Mock
  UserTenantService userTenantService;
  @Mock
  TenantService tenantService;
  @Mock
  TenantRepository tenantRepository;
  @Mock
  ConsortiumService consortiumService;
  @Mock
  KafkaService kafkaService;
  @Mock
  FolioExecutionContext folioExecutionContext;
  AutoCloseable mockitoMocks;

  private RecordHeader createKafkaHeader(String headerName, String headerValue) {
    return new RecordHeader(headerName, headerValue.getBytes(StandardCharsets.UTF_8));
  }

  @BeforeEach
  public void beforeEach() {
    mockitoMocks = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void afterEach() throws Exception {
    mockitoMocks.close();
  }

  @Test
  void primaryAffiliationAddedSuccessfullyTest() {
    var te = createTenantEntity();
    te.setId(TENANT);

    when(tenantService.getByTenantId(anyString())).thenReturn(te);
    doNothing().when(consortiumService).checkConsortiumExistsOrThrow(any());
    when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);
    Map<String, Collection<String>> map = createOkapiHeaders();
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(map);

    folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, map);
    try (var fec = new FolioExecutionContextSetter(folioExecutionContext)) {
      userAffiliationService.createPrimaryUserAffiliation(userCreatedEventSample);
    }

    verify(kafkaService, times(1)).send(any(), anyString(), any());

  }
  @Test
  void primaryAffiliationAddedSuccessfullyTestToCentralTenant() {
    var te = createTenantEntity();

    when(tenantService.getByTenantId(anyString())).thenReturn(te);
    doNothing().when(consortiumService).checkConsortiumExistsOrThrow(any());
    when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);
    Map<String, Collection<String>> map = createOkapiHeaders();
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(map);

    folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, map);
    try (var fec = new FolioExecutionContextSetter(folioExecutionContext)) {
      userAffiliationService.createPrimaryUserAffiliation(userCreatedEventSample);
    }

    verify(kafkaService, times(1)).send(any(), anyString(), any());
  }

  @Test
  void tenantNotInConsortiaWhenCreatingTest() {
    when(tenantRepository.findById(anyString())).thenReturn(null);

    Map<String, Collection<String>> map = createOkapiHeaders();
    folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, map);
    try (var fec = new FolioExecutionContextSetter(folioExecutionContext)) {
      userAffiliationService.createPrimaryUserAffiliation(userCreatedEventSample);
    }
    verify(kafkaService, times(0)).send(any(), anyString(), any());
  }

  @Test
  void primaryAffiliationAlreadyExists() {
    var te = createTenantEntity();

    when(tenantService.getByTenantId(anyString())).thenReturn(te);
    when(userTenantService.checkUserIfHasPrimaryAffiliationByUserId(any(), anyString())).thenReturn(true);

    Map<String, Collection<String>> map = createOkapiHeaders();
    folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, map);
    try (var fec = new FolioExecutionContextSetter(folioExecutionContext)) {
      userAffiliationService.createPrimaryUserAffiliation(userCreatedEventSample);
    }
    verify(kafkaService, times(0)).send(any(), anyString(), any());
  }

  @Test
  void primaryAffiliationSuccessfullyDeletedTest() {
    var te = createTenantEntity();

    when(tenantService.getByTenantId(anyString())).thenReturn(te);
    doNothing().when(consortiumService).checkConsortiumExistsOrThrow(any());
    when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);
    Map<String, Collection<String>> map = createOkapiHeaders();
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(map);

    folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, map);
    try (var fec = new FolioExecutionContextSetter(folioExecutionContext)) {
      userAffiliationService.deletePrimaryUserAffiliation(userDeletedEventSample);
    }

    verify(kafkaService, times(1)).send(any(), anyString(), any());
  }

  @Test
  void kafkaMessageFailedWhenDeletingTest() {
    var te = createTenantEntity();

    when(tenantService.getByTenantId(anyString())).thenReturn(te);
    doNothing().when(consortiumService).checkConsortiumExistsOrThrow(any());
    doThrow(new RuntimeException("Unable to send message to Kafka")).when(kafkaService).send(any(), anyString(), any());
    when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);
    Map<String, Collection<String>> map = createOkapiHeaders();
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(map);

    try (var fec = new FolioExecutionContextSetter(folioExecutionContext)) {
      userAffiliationService.deletePrimaryUserAffiliation(userDeletedEventSample);
    }

    verify(kafkaService, times(1)).send(any(), anyString(), any());
  }

  @Test
  void tenantNotInConsortiaWhenDeletingTest() {
    when(tenantRepository.findById(anyString())).thenReturn(null);

    Map<String, Collection<String>> map = createOkapiHeaders();
    folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, map);
    try (var fec = new FolioExecutionContextSetter(folioExecutionContext)) {
      userAffiliationService.deletePrimaryUserAffiliation(userDeletedEventSample);
    }
    verify(kafkaService, times(0)).send(any(), anyString(), any());
  }

  private Map<String, Collection<String>> createOkapiHeaders() {
    Map<String, Collection<String>> map = new HashMap<>();
    map.put(TENANT, List.of(TENANT));
    map.put(TOKEN, List.of(TOKEN));
    map.put(XOkapiHeaders.USER_ID, List.of(UUID.randomUUID().toString()));
    return map;
  }
}
