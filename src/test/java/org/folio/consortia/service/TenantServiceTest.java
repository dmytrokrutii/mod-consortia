package org.folio.consortia.service;

import static org.folio.consortia.utils.EntityUtils.createConsortiaConfiguration;
import static org.folio.consortia.utils.EntityUtils.createTenant;
import static org.folio.consortia.utils.EntityUtils.createTenantEntity;
import static org.folio.consortia.utils.InputOutputTestUtils.getMockDataAsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.folio.consortia.client.ConsortiaConfigurationClient;
import org.folio.consortia.client.PermissionsClient;
import org.folio.consortia.client.SyncPrimaryAffiliationClient;
import org.folio.consortia.client.UserTenantsClient;
import org.folio.consortia.client.UsersClient;
import org.folio.consortia.config.FolioExecutionContextHelper;
import org.folio.consortia.config.kafka.KafkaService;
import org.folio.consortia.domain.dto.PermissionUser;
import org.folio.consortia.domain.dto.PermissionUserCollection;
import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.domain.dto.User;
import org.folio.consortia.domain.dto.UserCollection;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.folio.consortia.repository.ConsortiumRepository;
import org.folio.consortia.repository.TenantRepository;
import org.folio.consortia.repository.UserTenantRepository;
import org.folio.consortia.service.impl.TenantServiceImpl;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@EnableAutoConfiguration(exclude = BatchAutoConfiguration.class)
@EntityScan(basePackageClasses = TenantEntity.class)
class TenantServiceTest {

  private final static String CONSORTIUM_ID = "7698e46-c3e3-11ed-afa1-0242ac120002";
  private static final String CENTRAL_TENANT_ID = "diku";
  @InjectMocks
  private TenantServiceImpl tenantService;
  @Mock
  private TenantRepository tenantRepository;
  @Mock
  private UserTenantRepository userTenantRepository;
  @Mock
  private UserAffiliationService userAffiliationService;
  @Mock
  private ConversionService conversionService;
  @Mock
  private ConsortiumRepository consortiumRepository;
  @Mock
  private ConsortiumService consortiumService;
  @Mock
  private FolioExecutionContext folioExecutionContext = new FolioExecutionContext() {};
  @Mock
  private ConsortiaConfigurationClient configurationClient;
  @Mock
  private KafkaService kafkaService;
  @Mock
  UsersClient usersClient;
  @Mock
  UserTenantService userTenantService;
  @Mock
  private PermissionsClient permissionsClient;
  @Mock
  private UserTenantsClient userTenantsClient;
  @Mock
  private PermissionUserService permissionUserService;
  @Mock
  private PermissionUserService permissionService;
  @Mock
  private UserService userService;
  @Mock
  private SyncPrimaryAffiliationClient syncPrimaryAffiliationClient;
  @Mock
  FolioModuleMetadata folioModuleMetadata;

  @Mock
  private FolioExecutionContextHelper contextHelper;

  @Test
  void shouldGetTenantList() {
    int offset = 0;
    int limit = 2;
    UUID consortiumId = UUID.randomUUID();
    TenantEntity tenantEntity1 = createTenantEntity("ABC1", "TestName1");
    TenantEntity tenantEntity2 = createTenantEntity("ABC1", "TestName2");
    List<TenantEntity> tenantEntityList = new ArrayList<>();
    tenantEntityList.add(tenantEntity1);
    tenantEntityList.add(tenantEntity2);

    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    when(tenantRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.findByConsortiumId(any(), any(PageRequest.of(offset, limit).getClass())))
      .thenReturn(new PageImpl<>(tenantEntityList, PageRequest.of(offset, limit), tenantEntityList.size()));

    var tenantCollection = tenantService.get(consortiumId, 0, 10);
    Assertions.assertEquals(2, tenantCollection.getTotalRecords());
  }

  @Test
  void shouldSaveNotCentralTenantWithNewUserAndPermissions() {
    UUID consortiumId = UUID.fromString(CONSORTIUM_ID);
    TenantEntity tenantEntity1 = createTenantEntity("ABC1", "TestName1");
    Tenant tenant = createTenant("TestID", "Test");
    TenantEntity centralTenant = createTenantEntity("diku", "diku");
    PermissionUserCollection permissionUserCollection = new PermissionUserCollection();
    permissionUserCollection.setPermissionUsers(List.of());
    User user = new User();
    user.setId(UUID.randomUUID().toString());

    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    when(userService.prepareShadowUser(any(), any())).thenReturn(user);
    when(userService.createUser(any())).thenReturn(user);
    when(userService.getById(any())).thenReturn(new User());
    when(permissionsClient.get(any())).thenReturn(permissionUserCollection);
    when(permissionsClient.create(any())).thenReturn(PermissionUser.of(UUID.randomUUID().toString(), user.getId(), List.of("users.collection.get")));
    when(tenantRepository.existsById(any())).thenReturn(false);
    when(tenantRepository.findCentralTenant()).thenReturn(Optional.of(centralTenant));
    when(tenantRepository.save(any(TenantEntity.class))).thenReturn(tenantEntity1);
    doNothing().when(configurationClient).saveConfiguration(createConsortiaConfiguration(CENTRAL_TENANT_ID));
    doNothing().when(userTenantsClient).postUserTenant(any());
    when(conversionService.convert(tenantEntity1, Tenant.class)).thenReturn(tenant);
    doReturn(folioExecutionContext).when(contextHelper).getSystemUserFolioExecutionContext(anyString());
    when(folioExecutionContext.getTenantId()).thenReturn("diku");

    var tenant1 = tenantService.save(consortiumId, UUID.randomUUID(), tenant);

    verify(userService).prepareShadowUser(any(), any());
    verify(userTenantRepository).save(any());
    verify(configurationClient).saveConfiguration(any());
    verify(userTenantsClient).postUserTenant(any());
    verify(userService).createUser(any());

    Assertions.assertEquals(tenant, tenant1);
  }

  @Test
  void shouldSaveCentralTenantWithExistingAndPermissions() throws JsonProcessingException {
    UUID consortiumId = UUID.fromString(CONSORTIUM_ID);
    TenantEntity tenantEntity1 = createTenantEntity("ABC1", "TestName1");
    Tenant tenant = createTenant("TestID", "Test", true);
    TenantEntity centralTenant = createTenantEntity("diku", "diku");
    PermissionUser permissionUser = new PermissionUser();
    permissionUser.setPermissions(List.of("users.collection.get"));
    PermissionUserCollection permissionUserCollection = new PermissionUserCollection();
    permissionUserCollection.setPermissionUsers(List.of(permissionUser));
    User user = new User();
    user.setId(UUID.randomUUID().toString());
    var userCollectionString = getMockDataAsString("mockdata/user_collection.json");
    UserCollection userCollection = new ObjectMapper().readValue(userCollectionString, UserCollection.class);

    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    when(userService.prepareShadowUser(any(), any())).thenReturn(user);
    when(permissionsClient.get(any())).thenReturn(permissionUserCollection);
    doNothing().when(permissionsClient).addPermission(any(), any());
    when(tenantRepository.existsById(any())).thenReturn(false);
    when(tenantRepository.findCentralTenant()).thenReturn(Optional.of(centralTenant));
    when(tenantRepository.save(any(TenantEntity.class))).thenReturn(tenantEntity1);
    doNothing().when(configurationClient).saveConfiguration(createConsortiaConfiguration(CENTRAL_TENANT_ID));
    doNothing().when(userTenantsClient).postUserTenant(any());
    when(conversionService.convert(tenantEntity1, Tenant.class)).thenReturn(tenant);
    doReturn(folioExecutionContext).when(contextHelper).getSystemUserFolioExecutionContext(anyString());
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);
    when(usersClient.getUserCollection(anyString(), anyInt(), anyInt())).thenReturn(userCollection);

    var tenant1 = tenantService.save(consortiumId, UUID.randomUUID(), tenant);

    verify(configurationClient).saveConfiguration(any());

    verify(userService, never()).prepareShadowUser(any(), any());
    verify(userTenantRepository, never()).save(any());
    verify(userTenantsClient, never()).postUserTenant(any());
    verify(userService, never()).createUser(any());
    verify(permissionUserService, never()).createWithPermissionsFromFile(any(), any());

    Assertions.assertEquals(tenant, tenant1);
  }


  @Test
  void shouldUpdateTenant() {
    UUID consortiumId = UUID.randomUUID();
    TenantEntity tenantEntity1 = createTenantEntity("TestID", "TestName1");
    Tenant tenant = createTenant("TestID", "TestName2");

    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);

    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    when(tenantRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.save(any(TenantEntity.class))).thenReturn(tenantEntity1);
    when(conversionService.convert(tenantEntity1, Tenant.class)).thenReturn(tenant);

    var tenant1 = tenantService.update(UUID.fromString(CONSORTIUM_ID), tenant.getId(), tenant);
    Assertions.assertEquals(tenant.getId(), tenant1.getId());
    Assertions.assertEquals("TestName2", tenant1.getName());
  }

  @Test
  void shouldDeleteTenant() {
    UUID consortiumId = UUID.randomUUID();
    String tenantId = "diku";

    doNothing().when(consortiumService).checkConsortiumExistsOrThrow(consortiumId);
    when(tenantRepository.existsById(any())).thenReturn(true);
    doNothing().when(tenantRepository).deleteById(tenantId);

    tenantService.delete(consortiumId, tenantId);

    // Assert
    Mockito.verify(consortiumService).checkConsortiumExistsOrThrow(consortiumId);
    Mockito.verify(tenantRepository).existsById(tenantId);
    Mockito.verify(tenantRepository).deleteById(tenantId);
  }

  @Test()
  void testDeleteWithAssociation() {
    UUID consortiumId = UUID.randomUUID();
    String tenantId = "123";

    // Mock repository method calls
    Mockito.when(tenantRepository.existsById(tenantId)).thenReturn(true);
    Mockito.when(userTenantRepository.existsByTenantId(tenantId)).thenReturn(true);

    // Call the method
    assertThrows(IllegalArgumentException.class, () ->
      tenantService.delete(consortiumId, tenantId));
  }

  @Test
  void testDeleteNonexistentTenant() {
    UUID consortiumId = UUID.randomUUID();
    String tenantId = "123";

    // Mock repository method calls
    when(tenantRepository.existsById(tenantId)).thenReturn(false);

    // Call the method
    assertThrows(ResourceNotFoundException.class, () ->
      tenantService.delete(consortiumId, tenantId));
  }

  @Test
  void shouldThrowExceptionWhileSavingLocalTenantWithoutAdminUserId() {
    TenantEntity tenantEntity1 = createTenantEntity("TestID", "TestName1");
    Tenant tenant = createTenant("TestID", "TestName2");

    when(consortiumRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.existsById(any())).thenReturn(false);
    when(tenantRepository.save(any(TenantEntity.class))).thenReturn(tenantEntity1);
    when(conversionService.convert(tenantEntity1, Tenant.class)).thenReturn(tenant);

    assertThrows(java.lang.IllegalArgumentException.class, () ->
      tenantService.save(UUID.fromString(CONSORTIUM_ID), null, tenant));
  }

  @Test
  void shouldThrowExceptionWhileSavingWithDuplicateCodeOrName() {
    TenantEntity tenantEntity1 = createTenantEntity("TestID", "TestName1");
    Tenant tenant = createTenant("TestID", "TestName2");

    when(consortiumRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.existsById(any())).thenReturn(false);
    when(tenantRepository.save(any(TenantEntity.class))).thenReturn(tenantEntity1);
    when(tenantRepository.existsByCode(any())).thenReturn(true);
    when(conversionService.convert(tenantEntity1, Tenant.class)).thenReturn(tenant);

    assertThrows(org.folio.consortia.exception.ResourceAlreadyExistException.class, () ->
      tenantService.save(UUID.fromString(CONSORTIUM_ID), UUID.randomUUID(), tenant));
  }

  @Test
  void shouldThrowExceptionWhileUpdateTenant() {
    TenantEntity tenantEntity1 = createTenantEntity("TestID", "TestName1");
    Tenant tenant = createTenant("TestID", "TestName2");

    when(consortiumRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.save(any(TenantEntity.class))).thenReturn(tenantEntity1);
    when(conversionService.convert(tenantEntity1, Tenant.class)).thenReturn(tenant);

    assertThrows(java.lang.IllegalArgumentException.class, () ->
      tenantService.update(UUID.fromString(CONSORTIUM_ID), tenant.getId() + "1234", tenant));
  }

  @Test
  void shouldThrowNotFoundExceptionWhileUpdateTenant() {
    TenantEntity tenantEntity1 = createTenantEntity("TestID", "TestName1");
    Tenant tenant = createTenant("TestID", "TestName2");

    when(consortiumRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.save(any(TenantEntity.class))).thenReturn(tenantEntity1);
    when(conversionService.convert(tenantEntity1, Tenant.class)).thenReturn(tenant);

    assertThrows(org.folio.consortia.exception.ResourceNotFoundException.class, () ->
      tenantService.update(UUID.fromString(CONSORTIUM_ID), tenant.getId() + "1234", tenant));
  }

  @Test
  void shouldThrowResourceAlreadyExistExceptionWhileSavingCentralTenant() {
    UUID consortiumId = UUID.fromString(CONSORTIUM_ID);
    Tenant tenant = createTenant("TestID", "Test", true);

    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    when(tenantRepository.existsById(any())).thenReturn(false);
    when(tenantRepository.existsByIsCentralTrue()).thenReturn(true);

    assertThrows(org.folio.consortia.exception.ResourceAlreadyExistException.class,
      () -> tenantService.save(UUID.fromString(CONSORTIUM_ID), null, tenant));
  }

  @Test
  void shouldNotSaveTenantForDuplicateId() {
    TenantEntity tenantEntity1 = createTenantEntity("TestID", "Test");
    Tenant tenant = createTenant("TestID", "Testq", true);
    TenantEntity centralTenant = createTenantEntity("diku", "diku");

    when(tenantRepository.existsById(any())).thenReturn(true);
    when(conversionService.convert(tenantEntity1, Tenant.class)).thenReturn(tenant);
    when(tenantRepository.findCentralTenant()).thenReturn(Optional.of(centralTenant));
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);

    assertThrows(org.folio.consortia.exception.ResourceAlreadyExistException.class, () ->
      tenantService.save(UUID.fromString(CONSORTIUM_ID), null, tenant));
  }

  @Test
  void shouldRetrieveEntityByTenantId() {
    when(tenantRepository.findById(anyString())).thenReturn(Optional.of(new TenantEntity()));
    var tenantEntity = tenantService.getByTenantId(UUID.randomUUID().toString());
    assertNotNull(tenantEntity);
  }

  @Test
  void shouldNotRetrieveEntityByTenantId() {
    when(tenantRepository.findById(anyString())).thenReturn(Optional.empty());
    var tenantEntity = tenantService.getByTenantId(UUID.randomUUID().toString());
    assertNull(tenantEntity);
  }
}
