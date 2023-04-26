package org.folio.consortia.service;

import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.consortia.config.kafka.KafkaService;
import org.folio.consortia.domain.dto.UserTenant;
import org.folio.consortia.domain.entity.TenantEntity;
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
    var te = getTenantEntity();

    when(tenantService.getByTenantId(anyString())).thenReturn(te);
    doNothing().when(consortiumService).checkConsortiumExistsOrThrow(any());

    Map<String, Collection<String>> map = new HashMap<>();
    map.put(TENANT, List.of(TENANT));
    map.put(TOKEN, List.of(TOKEN));
    map.put(XOkapiHeaders.USER_ID, List.of(UUID.randomUUID().toString()));
    folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, map);
    try (var fec = new FolioExecutionContextSetter(folioExecutionContext)) {
      userAffiliationService.createPrimaryUserAffiliation(userCreatedEventSample);
    }

    verify(kafkaService, times(1)).send(any(), anyString(), any());

  }

  @Test
  void tenantNotInConsortiaWhenCreatingTest() {
    when(tenantRepository.findById(anyString())).thenReturn(null);

    Map<String, Collection<String>> map = new HashMap<>();
    map.put(TENANT, List.of(TENANT));
    map.put(TOKEN, List.of(TOKEN));
    folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, map);
    try (var fec = new FolioExecutionContextSetter(folioExecutionContext)) {
      userAffiliationService.createPrimaryUserAffiliation(userCreatedEventSample);
    }
    verify(kafkaService, times(0)).send(any(), anyString(), any());
  }

  @Test
  void primaryAffiliationAlreadyExists() {
    var te = getTenantEntity();

    when(tenantService.getByTenantId(anyString())).thenReturn(te);
    when(userTenantService.getByUsernameAndTenantIdOrNull(any(), anyString(), anyString())).thenReturn(new UserTenant().isPrimary(true));

    Map<String, Collection<String>> map = new HashMap<>();
    map.put(TENANT, List.of(TENANT));
    map.put(TOKEN, List.of(TOKEN));
    folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, map);
    try (var fec = new FolioExecutionContextSetter(folioExecutionContext)) {
      userAffiliationService.createPrimaryUserAffiliation(userCreatedEventSample);
    }
    verify(kafkaService, times(0)).send(any(), anyString(), any());
  }

  @Test
  void primaryAffiliationSuccessfullyDeletedTest() {
    var te = getTenantEntity();
    when(tenantService.getByTenantId(anyString())).thenReturn(te);
    doNothing().when(consortiumService).checkConsortiumExistsOrThrow(any());

    Map<String, Collection<String>> map = new HashMap<>();
    map.put(TENANT, List.of(TENANT));
    map.put(TOKEN, List.of(TOKEN));
    map.put(XOkapiHeaders.USER_ID, List.of(UUID.randomUUID().toString()));
    folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, map);
    try (var fec = new FolioExecutionContextSetter(folioExecutionContext)) {
      userAffiliationService.deletePrimaryUserAffiliation(userDeletedEventSample);
    }
    verify(kafkaService, times(1)).send(any(), anyString(), any());
  }

  @Test
  void kafkaMessageFailedWhenDeletingTest() {
    var te = getTenantEntity();
    when(tenantService.getByTenantId(anyString())).thenReturn(te);
    doNothing().when(consortiumService).checkConsortiumExistsOrThrow(any());
    doThrow(new RuntimeException("Unable to send message to Kafka")).when(kafkaService).send(any(), anyString(), any());

    Map<String, Collection<String>> map = new HashMap<>();
    map.put(TENANT, List.of(TENANT));
    map.put(TOKEN, List.of(TOKEN));
    folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, map);
    try (var fec = new FolioExecutionContextSetter(folioExecutionContext)) {
      userAffiliationService.deletePrimaryUserAffiliation(userDeletedEventSample);
    }

    verify(kafkaService, times(1)).send(any(), anyString(), any());
  }
  @Test
  void tenantNotInConsortiaWhenDeletingTest() {
    when(tenantRepository.findById(anyString())).thenReturn(null);

    Map<String, Collection<String>> map = new HashMap<>();
    map.put(TENANT, List.of(TENANT));
    map.put(TOKEN, List.of(TOKEN));
    folioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, map);
    try (var fec = new FolioExecutionContextSetter(folioExecutionContext)) {
      userAffiliationService.deletePrimaryUserAffiliation(userDeletedEventSample);
    }
    verify(kafkaService, times(0)).send(any(), anyString(), any());
  }

  private TenantEntity getTenantEntity() {
    var te = new TenantEntity();
    te.setId(UUID.randomUUID().toString());
    te.setConsortiumId(UUID.randomUUID());
    return te;
  }
}
