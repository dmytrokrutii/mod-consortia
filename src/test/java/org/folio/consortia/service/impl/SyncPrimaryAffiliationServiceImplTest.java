package org.folio.consortia.service.impl;

import static org.folio.consortia.utils.EntityUtils.createTenantEntity;
import static org.folio.consortia.utils.InputOutputTestUtils.getMockDataAsString;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.consortia.client.SyncPrimaryAffiliationClient;
import org.folio.consortia.config.kafka.KafkaService;
import org.folio.consortia.domain.dto.SyncPrimaryAffiliationBody;
import org.folio.consortia.domain.dto.SyncUser;
import org.folio.consortia.domain.dto.TenantDetails.SetupStatusEnum;
import org.folio.consortia.domain.dto.User;
import org.folio.consortia.domain.dto.UserCollection;
import org.folio.consortia.domain.dto.UserTenant;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.domain.entity.UserTenantEntity;
import org.folio.consortia.repository.TenantRepository;
import org.folio.consortia.repository.UserTenantRepository;
import org.folio.consortia.service.ConsortiaConfigurationService;
import org.folio.consortia.service.TenantService;
import org.folio.consortia.service.UserService;
import org.folio.consortia.service.UserTenantService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;

@SpringBootTest
@EnableAutoConfiguration(exclude = BatchAutoConfiguration.class)
@EntityScan(basePackageClasses = UserTenantEntity.class)
class SyncPrimaryAffiliationServiceImplTest {
  @InjectMocks
  SyncPrimaryAffiliationServiceImpl syncPrimaryAffiliationService;
  @Mock
  private UserTenantService userTenantService;
  @Mock
  private TenantService tenantService;
  @Mock
  private KafkaService kafkaService;
  @Mock
  private UserTenantRepository userTenantRepository;
  @Mock
  private ConsortiaConfigurationService consortiaConfigurationService;
  @Mock
  private SyncPrimaryAffiliationClient syncClient;

  @Mock
  UserService userService;
  @Mock
  TenantRepository tenantRepository;

  protected static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6IjFkM2I1OGNiLTA3YjUtNWZjZC04YTJhLTNjZTA2YTBlYjkwZiIsImlhdCI6MTYxNjQyMDM5MywidGVuYW50IjoiZGlrdSJ9.2nvEYQBbJP1PewEgxixBWLHSX_eELiBEBpjufWiJZRs";
  protected static final String TENANT = "diku";

  @Test
  void createPrimaryUserAffiliationsWhenCentralTenantSaving() throws JsonProcessingException {
    var consortiumId = UUID.randomUUID();
    var tenantId = "ABC1";
    var centralTenantId = "diku";
    TenantEntity tenantEntity1 = createTenantEntity(tenantId, "TestName1");
    tenantEntity1.setConsortiumId(consortiumId);

    var userCollectionString = getMockDataAsString("mockdata/user_collection.json");
    List<User> userCollection = new ObjectMapper().readValue(userCollectionString, UserCollection.class).getUsers();

    var syncUser = new SyncUser().id(UUID.randomUUID()
        .toString())
      .username("test_user");
    var spab = new SyncPrimaryAffiliationBody()
      .users(Collections.singletonList(syncUser))
      .tenantId(tenantId);

    // stub collection of 2 users
    when(tenantService.getByTenantId(anyString())).thenReturn(tenantEntity1);
    when(userTenantRepository.findByUserId(any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));
    when(tenantRepository.findById(anyString())).thenReturn(Optional.of(tenantEntity1));
    when(userService.getUsersByQuery(anyString(), anyInt(), anyInt())).thenReturn(userCollection);
    when(userTenantService.createPrimaryUserTenantAffiliation(any(), any(), anyString(), anyString())).thenReturn(new UserTenant());
    when(consortiaConfigurationService.getCentralTenantId(anyString())).thenReturn(tenantId);

    syncPrimaryAffiliationService.createPrimaryUserAffiliations(consortiumId, centralTenantId, spab);

    verify(kafkaService, timeout(2000)).send(any(), anyString(), anyString());
    verify(tenantService).updateTenantSetupStatus(tenantId, centralTenantId, SetupStatusEnum.COMPLETED);
  }
  @Test
  void createPrimaryUserAffiliationsWhenLocalTenantSaving() throws JsonProcessingException {
    var consortiumId = UUID.randomUUID();
    var tenantId = "ABC1";
    var centralTenantId = "diku";
    TenantEntity tenantEntity1 = createTenantEntity(tenantId, "TestName1");
    tenantEntity1.setConsortiumId(consortiumId);

    var userCollectionString = getMockDataAsString("mockdata/user_collection.json");
    List<User> userCollection = new ObjectMapper().readValue(userCollectionString, UserCollection.class).getUsers();

    var syncUser = new SyncUser().id(UUID.randomUUID()
        .toString())
      .username("test_user");
    var spab = new SyncPrimaryAffiliationBody()
      .users(Collections.singletonList(syncUser))
      .tenantId(tenantId);

    // stub collection of 2 users
    when(tenantService.getByTenantId(anyString())).thenReturn(tenantEntity1);
    when(userTenantRepository.findByUserId(any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));
    when(tenantRepository.findById(anyString())).thenReturn(Optional.of(tenantEntity1));
    when(userService.getUsersByQuery(anyString(), anyInt(), anyInt())).thenReturn(userCollection);
    when(userTenantService.createPrimaryUserTenantAffiliation(any(), any(), anyString(), anyString())).thenReturn(new UserTenant());
    when(consortiaConfigurationService.getCentralTenantId(anyString())).thenReturn(centralTenantId);
    when(userTenantService.save(any(), any(), anyBoolean())).thenReturn(new UserTenant());

    syncPrimaryAffiliationService.createPrimaryUserAffiliations(consortiumId, centralTenantId, spab);

    verify(kafkaService, timeout(2000)).send(any(), anyString(), anyString());
    verify(tenantService).updateTenantSetupStatus(tenantId, centralTenantId, SetupStatusEnum.COMPLETED);
  }

  @Test
  void syncPrimaryUserAffiliationsWhenTenantSaving() throws JsonProcessingException {
    var consortiumId = UUID.randomUUID();
    var tenantId = "ABC1";
    var centralTenantId = "diku";
    TenantEntity tenantEntity1 = createTenantEntity(tenantId, "TestName1");
    tenantEntity1.setConsortiumId(consortiumId);

    var userCollectionString = getMockDataAsString("mockdata/user_collection.json");
    List<User> userCollection = new ObjectMapper().readValue(userCollectionString, UserCollection.class).getUsers();

    var syncUser = new SyncUser().id("88888888-8888-4888-8888-888888888888").username("mockuser8");
    var syncUser2 = new SyncUser().id("99999999-9999-4999-9999-999999999999").username("mockuser9");
    var spab = new SyncPrimaryAffiliationBody()
      .users(List.of(syncUser, syncUser2))
      .tenantId(tenantId);

    // stub collection of 2 users
    when(userService.getUsersByQuery(anyString(), anyInt(), anyInt())).thenReturn(userCollection);
    when(userTenantService.createPrimaryUserTenantAffiliation(any(), any(), anyString(), anyString())).thenReturn(new UserTenant());

    syncPrimaryAffiliationService.syncPrimaryAffiliations(consortiumId, tenantId, centralTenantId);

    verify(syncClient, timeout(2000)).savePrimaryAffiliations(spab, String.valueOf(consortiumId), tenantId, centralTenantId);
    verify(tenantService, never()).updateTenantSetupStatus(any(), any(), any());
  }

  @Test
  void syncPrimaryAffiliationsException() throws JsonProcessingException {
    var consortiumId = UUID.randomUUID();
    var tenantId = "ABC1";
    var centralTenantId = "diku";

    var userCollectionString = getMockDataAsString("mockdata/user_collection.json");
    List<User> userCollection = new ObjectMapper().readValue(userCollectionString, UserCollection.class).getUsers();

    var syncUser = new SyncUser().id("88888888-8888-4888-8888-888888888888").username("mockuser8");
    var syncUser2 = new SyncUser().id("99999999-9999-4999-9999-999999999999").username("mockuser9");
    var spab = new SyncPrimaryAffiliationBody()
      .users(List.of(syncUser, syncUser2))
      .tenantId(tenantId);

    // stub collection of 2 users
    when(userService.getUsersByQuery(anyString(), anyInt(), anyInt())).thenReturn(userCollection);
    when(syncClient.savePrimaryAffiliations(spab, String.valueOf(consortiumId), tenantId, centralTenantId))
      .thenThrow(FeignException.FeignClientException.class);

    try {
      syncPrimaryAffiliationService.syncPrimaryAffiliations(consortiumId, tenantId, centralTenantId);
      fail("Expected exception was not thrown");
    } catch (FeignException e) {
      verify(tenantService).updateTenantSetupStatus(tenantId, centralTenantId, SetupStatusEnum.FAILED);
    }
  }

  @Test
  void syncPrimaryAffiliationsGetUsersException() {
    var consortiumId = UUID.randomUUID();
    var tenantId = "ABC1";
    var centralTenantId = "diku";

    when(userService.getUsersByQuery(anyString(), anyInt(), anyInt()))
      .thenThrow(FeignException.FeignClientException.class);

    syncPrimaryAffiliationService.syncPrimaryAffiliations(consortiumId, tenantId, centralTenantId);

    verify(tenantService).updateTenantSetupStatus(tenantId, centralTenantId, SetupStatusEnum.FAILED);
  }

  @Test
  void createPrimaryAffiliationsException() {
    var consortiumId = UUID.randomUUID();
    var tenantId = "ABC1";
    var centralTenantId = "diku";
    var syncUser = new SyncUser().id(UUID.randomUUID()
        .toString())
      .username("test_user");
    var spab = new SyncPrimaryAffiliationBody()
      .users(Collections.singletonList(syncUser))
      .tenantId(tenantId);

    when(tenantService.getByTenantId(anyString())).thenThrow(DataAccessResourceFailureException.class);

    try {
      syncPrimaryAffiliationService.createPrimaryUserAffiliations(consortiumId, centralTenantId, spab);
      fail("Expected exception was not thrown");
    } catch (DataAccessResourceFailureException e) {
      verifyNoInteractions(kafkaService);
      verify(tenantService).updateTenantSetupStatus(tenantId, centralTenantId, SetupStatusEnum.FAILED);
    }
  }

  @Test
  void createPrimaryAffiliationsPartialFailure() throws JsonProcessingException {
    var consortiumId = UUID.randomUUID();
    var tenantId = "ABC1";
    var centralTenantId = "diku";
    TenantEntity tenantEntity1 = createTenantEntity(tenantId, "TestName1");
    tenantEntity1.setConsortiumId(consortiumId);

    var userCollectionString = getMockDataAsString("mockdata/user_collection.json");
    List<User> userCollection = new ObjectMapper().readValue(userCollectionString, UserCollection.class).getUsers();

    var syncUser = new SyncUser().id("88888888-8888-4888-8888-888888888888").username("mockuser8");
    var syncUser2 = new SyncUser().id("99999999-9999-4999-9999-999999999999").username("mockuser9");
    var spab = new SyncPrimaryAffiliationBody()
      .users(List.of(syncUser, syncUser2))
      .tenantId(tenantId);

    when(tenantService.getByTenantId(anyString())).thenReturn(tenantEntity1);
    when(userTenantRepository.findByUserId(any(), any())).thenReturn(new PageImpl<>(Collections.emptyList()));
    when(tenantRepository.findById(anyString())).thenReturn(Optional.of(tenantEntity1));

    when(userService.getUsersByQuery(anyString(), anyInt(), anyInt())).thenReturn(userCollection);
    when(userTenantService.createPrimaryUserTenantAffiliation(any(), any(), anyString(), anyString()))
      .thenThrow(DataAccessResourceFailureException.class);
    when(consortiaConfigurationService.getCentralTenantId(anyString())).thenReturn(tenantId);

    syncPrimaryAffiliationService.createPrimaryUserAffiliations(consortiumId, centralTenantId, spab);
    verifyNoInteractions(kafkaService);
    verify(tenantService).updateTenantSetupStatus(tenantId, centralTenantId, SetupStatusEnum.COMPLETED_WITH_ERRORS);
  }
}
