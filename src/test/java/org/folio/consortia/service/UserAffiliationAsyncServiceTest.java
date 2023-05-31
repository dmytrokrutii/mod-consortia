package org.folio.consortia.service;

import static org.folio.consortia.utils.EntityUtils.createTenant;
import static org.folio.consortia.utils.EntityUtils.createTenantEntity;
import static org.folio.consortia.utils.EntityUtils.createUserTenantEntity;
import static org.folio.consortia.utils.InputOutputTestUtils.getMockData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.consortia.config.kafka.KafkaService;
import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.domain.dto.User;
import org.folio.consortia.domain.dto.UserCollection;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.domain.entity.UserTenantEntity;
import org.folio.consortia.repository.UserTenantRepository;
import org.folio.consortia.service.impl.UserAffiliationAsyncServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@EnableAutoConfiguration(exclude = BatchAutoConfiguration.class)
@EntityScan(basePackageClasses = TenantEntity.class)
class UserAffiliationAsyncServiceTest {
  @InjectMocks
  UserAffiliationAsyncServiceImpl userAffiliationAsyncService;
  @Mock
  UserService userService;
  @Mock
  UserTenantRepository userTenantRepository;
  @Mock
  UserTenantService userTenantService;
  @Mock
  KafkaService kafkaService;
  @Autowired
  ObjectMapper objectMapper;

  @Test
  void createPrimaryUserAffiliationsAsyncSuccessTest() throws JsonProcessingException {
    UUID consortiumId = UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002");
    TenantEntity tenantEntity1 = createTenantEntity("ABC1", "TestName1");
    tenantEntity1.setConsortiumId(consortiumId);
    UserTenantEntity userTenantEntity = createUserTenantEntity(UUID.randomUUID());
    Tenant tenant = createTenant("TestID", "Test");

    var userCollectionString = getMockData("mockdata/user_collection.json");
    List<User> userCollection = new ObjectMapper().readValue(userCollectionString, UserCollection.class).getUsers();

    // stub collection of 2 users
    when(userService.getUsersByQuery(anyString(), anyInt(), anyInt())).thenReturn(userCollection);
    when(userTenantRepository.findByUserIdAndTenantId(any(), anyString())).thenReturn(Optional.of(userTenantEntity));

    userAffiliationAsyncService.createPrimaryUserAffiliationsAsync(consortiumId, tenantEntity1, tenant)
      .join();
    verify(userTenantService, times(2)).createPrimaryUserTenantAffiliation(any(), any(), anyString(), anyString());
    verify(kafkaService, times(2)).send(any(), anyString(), anyString());
  }
}
