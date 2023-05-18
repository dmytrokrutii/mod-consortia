package org.folio.consortia.service;

import feign.FeignException;
import org.folio.consortia.client.PermissionsClient;
import org.folio.consortia.client.UsersClient;
import org.folio.consortia.domain.converter.UserTenantConverter;
import org.folio.consortia.domain.dto.Personal;
import org.folio.consortia.domain.dto.User;
import org.folio.consortia.domain.dto.UserEvent;
import org.folio.consortia.domain.dto.UserTenant;
import org.folio.consortia.domain.dto.UserTenantCollection;
import org.folio.consortia.domain.dto.Userdata;
import org.folio.consortia.domain.entity.*;
import org.folio.consortia.domain.dto.PermissionUser;
import org.folio.consortia.domain.dto.PermissionUserCollection;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.folio.consortia.repository.ConsortiumRepository;
import org.folio.consortia.repository.UserTenantRepository;
import org.folio.consortia.service.impl.UserTenantServiceImpl;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnableAutoConfiguration(exclude = BatchAutoConfiguration.class)
@EntityScan(basePackageClasses = UserTenantEntity.class)
class UserTenantServiceTest {

  private static final String CONSORTIUM_ID = "7698e46-c3e3-11ed-afa1-0242ac120002";

  @InjectMocks
  private UserTenantServiceImpl userTenantService;
  @Mock
  private UserTenantRepository userTenantRepository;
  @Mock
  private ConversionService conversionService;
  @Mock
  private ConsortiumRepository consortiumRepository;
  @Mock
  private ConsortiumService consortiumService;
  @Mock
  private UsersClient usersClient;
  @Mock
  private PermissionsClient permissionsClient;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private FolioModuleMetadata folioModuleMetadata;

  /* Success cases */
  @Test
  void shouldGetUserTenantList() {
    // given
    List<UserTenantEntity> userTenantEntities = List.of(new UserTenantEntity(), new UserTenantEntity());
    Page<UserTenantEntity> userTenantPage = new PageImpl<>(userTenantEntities, PageRequest.of(0, 10), userTenantEntities.size());

    when(consortiumRepository.findById(UUID.fromString(CONSORTIUM_ID)))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(userTenantRepository.findAll(PageRequest.of(0, 10))).thenReturn(userTenantPage);

    // when
    var result = userTenantService.get(UUID.fromString(CONSORTIUM_ID), 0, 10);

    // then
    assertEquals(userTenantEntities.size(), result.getUserTenants().size());
    assertEquals(2, result.getTotalRecords());
  }

  @Test
  void shouldGetUserTenantByAssociationId() {
    // given
    UUID associationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String tenantId = String.valueOf(UUID.randomUUID());
    UserTenantEntity userTenant = createUserTenantEntity(associationId, userId, "testuser", tenantId);
    List<UserTenantEntity> userTenantEntities = List.of(userTenant);

    when(consortiumRepository.findById(UUID.fromString(CONSORTIUM_ID)))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(conversionService.convert(userTenant, UserTenant.class)).thenReturn(toDto(userTenant));
    when(userTenantRepository.findById(associationId)).thenReturn(Optional.of(userTenantEntities.get(0)));

    // when
    var result = userTenantService.getById(UUID.fromString(CONSORTIUM_ID), associationId);

    // then
    assertEquals(associationId, result.getId());
    assertEquals("testuser", result.getUsername());
  }

  @Test
  void shouldGetUserTenantListByUserId() {
    // given
    UUID userId = UUID.randomUUID();
    UUID associationId = UUID.randomUUID();
    String tenantId = String.valueOf(UUID.randomUUID());

    UserTenantEntity userTenant = createUserTenantEntity(associationId, userId, "testuser", tenantId);
    UserTenantEntity userTenant2 = createUserTenantEntity(associationId, userId, "testuser", tenantId);
    List<UserTenantEntity> userTenantEntities = List.of(userTenant);

    when(consortiumRepository.findById(UUID.fromString(CONSORTIUM_ID)))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(conversionService.convert(userTenant, UserTenant.class)).thenReturn(toDto(userTenant));
    when(conversionService.convert(userTenant2, UserTenant.class)).thenReturn(toDto(userTenant2));
    when(userTenantRepository.findByUserId(userId, PageRequest.of(0, 10)))
      .thenReturn(new PageImpl<>(userTenantEntities, PageRequest.of(0, 10), userTenantEntities.size()));

    // when
    UserTenantCollection result = userTenantService.getByUserId(UUID.fromString(CONSORTIUM_ID), userId, 0, 10);

    // then
    assertEquals(userTenant2, userTenant);
    assertEquals(userTenantEntities.size(), result.getUserTenants().size());
    assertEquals(1, result.getTotalRecords());
  }

  @Test
  void shouldGetUserTenantByUsernameAndTenantId() {
    // given
    UUID userId = UUID.randomUUID();
    UUID associationId = UUID.randomUUID();
    String tenantId = String.valueOf(UUID.randomUUID());
    UserTenantEntity userTenant = createUserTenantEntity(associationId, userId, "testuser", tenantId);

    when(consortiumRepository.findById(UUID.fromString(CONSORTIUM_ID)))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(conversionService.convert(userTenant, UserTenant.class)).thenReturn(toDto(userTenant));
    when(userTenantRepository.findByUsernameAndTenantId("testuser", tenantId)).thenReturn(Optional.of(userTenant));

    // when
    UserTenantCollection result = userTenantService.getByUsernameAndTenantId(UUID.fromString(CONSORTIUM_ID), "testuser", tenantId);

    // then
    assertEquals(tenantId, result.getUserTenants().get(0).getTenantId());
    assertEquals(1, result.getTotalRecords());
  }

  @Test
  void shouldSavePrimaryAffiliation() {
    var consId = UUID.randomUUID();
    var userEvent = createUserEvent();
    ArgumentCaptor<UserTenantEntity> argCaptor = ArgumentCaptor.forClass(UserTenantEntity.class);
    when(userTenantRepository.save(argCaptor.capture())).thenAnswer(i -> i.getArguments()[0]);

    var result = userTenantService.createPrimaryUserTenantAffiliation(consId, new TenantEntity(), userEvent);
    assertNull(result);
  }

  @Test
  void shouldDeletePrimaryAffiliation() {
    var userEvent = createUserEvent();
    doNothing().when(userTenantRepository).deleteByUserIdAndIsPrimaryTrue(any());

    userTenantService.deletePrimaryUserTenantAffiliation(UUID.fromString(userEvent.getUserDto().getId()));

    verify(userTenantRepository, times(1)).deleteByUserIdAndIsPrimaryTrue(any());
  }

  @Test
  void shouldSaveUserTenant() {
    UserTenant tenant = createUserTenantDtoEntity();
    UUID associationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String tenantId = String.valueOf(UUID.randomUUID());
    UserTenantEntity userTenant = createUserTenantEntity(associationId, userId, "testuser", tenantId);
    userTenant.setIsPrimary(true);

    when(consortiumRepository.findById(UUID.fromString(CONSORTIUM_ID)))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(userTenantRepository.findByUserIdAndIsPrimary(any(), any())).thenReturn(Optional.of(userTenant));
    when(usersClient.getUsersByUserId(any())).thenReturn(createUserEntity(false));
    doNothing().when(usersClient).updateUser(any(), any(User.class));
    when(userTenantRepository.save(userTenant)).thenReturn(userTenant);
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);

    assertDoesNotThrow(() -> userTenantService.save(UUID.fromString(CONSORTIUM_ID), tenant));
  }

  @Test
  void shouldUpdateUserAndSaveUserTenant() {
    UserTenant tenant = createUserTenantDtoEntity();
    UUID associationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String tenantId = String.valueOf(UUID.randomUUID());
    UserTenantEntity userTenant = createUserTenantEntity(associationId, userId, "testuser", tenantId);
    userTenant.setIsPrimary(true);

    when(consortiumRepository.findById(UUID.fromString(CONSORTIUM_ID)))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(userTenantRepository.findByUserIdAndIsPrimary(any(), any())).thenReturn(Optional.of(userTenant));
    when(usersClient.getUsersByUserId(any())).thenReturn(createUserEntity(true));
    doNothing().when(usersClient).updateUser(any(), any(User.class));
    when(userTenantRepository.save(userTenant)).thenReturn(userTenant);
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);

    assertDoesNotThrow(() -> userTenantService.save(UUID.fromString(CONSORTIUM_ID), tenant));
  }

  @Test
  void shouldCreateUserWithExistingPermissionSetAndSaveUserTenant() {
    UserTenant tenant = createUserTenantDtoEntity();
    UUID associationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String tenantId = String.valueOf(UUID.randomUUID());
    UserTenantEntity userTenant = createUserTenantEntity(associationId, userId, "testuser", tenantId);
    userTenant.setIsPrimary(false);
    PermissionUser permissionUser = new PermissionUser();
    PermissionUserCollection permissionUserCollection = new PermissionUserCollection();
    permissionUserCollection.setPermissionUsers(List.of(permissionUser));
    when(consortiumRepository.findById(UUID.fromString(CONSORTIUM_ID)))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(userTenantRepository.findByUserIdAndIsPrimary(any(), any())).thenReturn(Optional.of(userTenant));
    when(usersClient.getUsersByUserId(any())).thenReturn(createNullUserEntity());
    when(permissionsClient.get(any())).thenReturn(permissionUserCollection);
    doNothing().when(usersClient).saveUser(any());
    when(userTenantRepository.save(userTenant)).thenReturn(userTenant);
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);

    assertDoesNotThrow(() -> userTenantService.save(UUID.fromString(CONSORTIUM_ID), tenant));
  }

  @Test
  void shouldCreateUserWithNewPermissionSetAndSaveUserTenant() {
    UserTenant tenant = createUserTenantDtoEntity();
    UUID associationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String tenantId = String.valueOf(UUID.randomUUID());
    UserTenantEntity userTenant = createUserTenantEntity(associationId, userId, "testuser", tenantId);
    userTenant.setIsPrimary(false);
    PermissionUser permissionUser = new PermissionUser();
    PermissionUserCollection permissionUserCollection = new PermissionUserCollection();
    permissionUserCollection.setPermissionUsers(List.of());
    when(consortiumRepository.findById(UUID.fromString(CONSORTIUM_ID)))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(userTenantRepository.findByUserIdAndIsPrimary(any(), any())).thenReturn(Optional.of(userTenant));
    when(usersClient.getUsersByUserId(any())).thenReturn(createNullUserEntity());
    when(permissionsClient.get(any())).thenReturn(permissionUserCollection);
    when(permissionsClient.create(any())).thenReturn(permissionUser);
    doNothing().when(usersClient).saveUser(any());
    when(userTenantRepository.save(userTenant)).thenReturn(userTenant);
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);

    assertDoesNotThrow(() -> userTenantService.save(UUID.fromString(CONSORTIUM_ID), tenant));
  }

  @Test
  void shouldRemoveAllOrphanedShadowUsers() {
    UUID associationId = UUID.randomUUID();
    UUID userId1 = UUID.randomUUID();
    String tenantId1 = String.valueOf(UUID.randomUUID());
    String tenantId2 = String.valueOf(UUID.randomUUID());
    UserTenantEntity userTenant1 = createUserTenantEntity(associationId, userId1, "testuser1", tenantId1);
    UserTenantEntity userTenant2 = createUserTenantEntity(associationId, userId1, "testuser2", tenantId1);
    UserTenantEntity userTenant3 = createUserTenantEntity(associationId, userId1, "testuser3", tenantId2);
    userTenant1.setIsPrimary(false);
    userTenant2.setIsPrimary(false);
    userTenant3.setIsPrimary(false);
    when(userTenantRepository.getByUserIdAndIsPrimaryFalse(any())).thenReturn(List.of(userTenant1, userTenant2, userTenant3));
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);

    assertDoesNotThrow(() -> userTenantService.deleteShadowUsers(userId1));
  }

  @Test
  void shouldDeleteUserTenantByUserIdAndTenantId() {
    UUID userId = UUID.randomUUID();
    String tenantId = "dikue";
    UUID associationId = UUID.randomUUID();
    UserTenantEntity userTenant = createUserTenantEntity(associationId, userId, "user", tenantId);
    userTenant.setIsPrimary(false);

    when(consortiumRepository.existsById(UUID.fromString(CONSORTIUM_ID))).thenReturn(true);
    when(usersClient.getUsersByUserId(any())).thenReturn(createNullUserEntity());
    when(userTenantRepository.findByUserIdAndTenantId(userId, tenantId))
      .thenReturn(Optional.of(userTenant));
    doNothing().when(userTenantRepository).deleteByUserIdAndTenantId(userId, tenantId);
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);

    assertDoesNotThrow(() -> userTenantService.deleteByUserIdAndTenantId(UUID.fromString(CONSORTIUM_ID), tenantId, userId));
  }

  /* Exception Cases */
  @Test
  void shouldThrowIllegalArgumentException() {
    when(consortiumRepository.findById(UUID.fromString(CONSORTIUM_ID)))
      .thenReturn(Optional.of(createConsortiumEntity()));
    UUID id = UUID.fromString(CONSORTIUM_ID);
    Assertions.assertThrows(IllegalArgumentException.class, () -> userTenantService.get(id, 0, 0));
  }

  @Test
  void shouldReturn404UserIdNotFoundException() {
    // given
    UUID userId = UUID.randomUUID();

    when(consortiumRepository.findById(UUID.fromString(CONSORTIUM_ID)))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(userTenantRepository.findByUserId(userId, PageRequest.of(0, 10)))
      .thenReturn(new PageImpl<>(new ArrayList<>()));

    UUID id = UUID.fromString(CONSORTIUM_ID);
    // throw exception
    assertThrows(ResourceNotFoundException.class, () -> userTenantService.getByUserId(id, userId, 0, 10));
  }

  @Test
  void shouldReturn404UsernameNotFoundException() {
    // given
    String username = "testuser";
    String tenantId = String.valueOf(UUID.randomUUID());

    when(consortiumRepository.findById(UUID.fromString(CONSORTIUM_ID)))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(userTenantRepository.findByUsernameAndTenantId(username, tenantId))
      .thenReturn(Optional.empty());
    UUID id = UUID.fromString(CONSORTIUM_ID);

    // throw exception
    assertThrows(ResourceNotFoundException.class,
      () -> userTenantService.getByUsernameAndTenantId(id, "testusername", tenantId));
  }
  @Test
  void shouldThrowNotFoundPrimaryAffiliationException() {
    UserTenant tenant = createUserTenantDtoEntity();
    UUID associationId = UUID.randomUUID();

    when(consortiumRepository.findById(UUID.fromString(CONSORTIUM_ID)))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(userTenantRepository.findByUserIdAndIsPrimary(any(), any())).thenReturn(Optional.empty());
    when(usersClient.getUsersByUserId(any())).thenReturn(createUserEntity(false));
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);

    assertThrows(org.folio.consortia.exception.ResourceNotFoundException.class,
      () -> userTenantService.save(associationId, tenant));
  }

  /* Error Cases */
  @Test
  void getByUsernameAndTenantIdNotFound() {
    doNothing().when(consortiumService).checkConsortiumExistsOrThrow(any());
    when(userTenantRepository.findByUserIdAndIsPrimary(any(), eq(true))).thenReturn(Optional.empty());

    var result = userTenantService.checkUserIfHasPrimaryAffiliationByUserId(UUID.randomUUID(), String.valueOf(UUID.randomUUID()));

    assertFalse(result);
  }

  @Test
  void shouldFailWhileDeletingUserTenantByUserIdAndTenantId() {
    UUID userId = UUID.randomUUID();
    String tenantId = "dikue";
    UUID associationId = UUID.randomUUID();
    UserTenantEntity userTenant = createUserTenantEntity(associationId, userId, "user", tenantId);
    userTenant.setIsPrimary(true);

    when(consortiumRepository.existsById(UUID.fromString(CONSORTIUM_ID))).thenReturn(true);
    when(usersClient.getUsersByUserId(any())).thenReturn(createUserEntity(true));
    when(userTenantRepository.findByUserIdAndTenantId(userId, tenantId))
      .thenReturn(Optional.of(userTenant));
    doNothing().when(userTenantRepository).deleteByUserIdAndTenantId(userId, tenantId);
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);

    assertThrows(org.folio.consortia.exception.PrimaryAffiliationException.class,
      () -> userTenantService.deleteByUserIdAndTenantId(UUID.fromString(CONSORTIUM_ID), tenantId, userId));
  }


  @Test
  void shouldThrowConsortiumClientException() {
    UserTenant tenant = createUserTenantDtoEntity();
    UUID associationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String tenantId = String.valueOf(UUID.randomUUID());
    UserTenantEntity userTenant = createUserTenantEntity(associationId, userId, "testuser", tenantId);
    userTenant.setIsPrimary(true);

    when(consortiumRepository.findById(UUID.fromString(CONSORTIUM_ID)))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(userTenantRepository.findByUserIdAndIsPrimary(any(), any())).thenReturn(Optional.of(userTenant));
    when(usersClient.getUsersByUserId(any())).thenThrow(FeignException.Forbidden.class);
    doNothing().when(usersClient).updateUser(any(), any(User.class));
    when(userTenantRepository.save(userTenant)).thenReturn(userTenant);
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);

    assertThrows(org.folio.consortia.exception.ConsortiumClientException.class,
      () -> userTenantService.save(UUID.fromString(CONSORTIUM_ID), tenant));
  }


  @ParameterizedTest
  @ValueSource(strings = {"testuser1", "testuser2", "testuser3"})
  void shouldThrowNotFoundIfUserNotFound(String username) {
    UserTenant tenant = createUserTenantDtoEntity();
    UUID associationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String tenantId = String.valueOf(UUID.randomUUID());
    UserTenantEntity userTenant = createUserTenantEntity(associationId, userId, username, tenantId);
    userTenant.setIsPrimary(true);

    when(consortiumRepository.findById(UUID.fromString(CONSORTIUM_ID)))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(userTenantRepository.findByUserIdAndIsPrimary(any(), any())).thenReturn(Optional.of(userTenant));
    when(usersClient.getUsersByUserId(any())).thenThrow(FeignException.NotFound.class);
    doNothing().when(usersClient).updateUser(any(), any(User.class));
    when(userTenantRepository.save(userTenant)).thenReturn(userTenant);
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);

    assertThrows(org.folio.consortia.exception.ResourceNotFoundException.class,
      () -> userTenantService.save(UUID.fromString(CONSORTIUM_ID), tenant));
  }

  @ParameterizedTest
  @ValueSource(strings = {"testuser1"})
  void shouldThrowIllegalStateExceptionFromUserClient(String username) {
    UserTenant tenant = createUserTenantDtoEntity();
    UUID associationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String tenantId = String.valueOf(UUID.randomUUID());
    UserTenantEntity userTenant = createUserTenantEntity(associationId, userId, username, tenantId);
    userTenant.setIsPrimary(true);

    when(consortiumRepository.findById(UUID.fromString(CONSORTIUM_ID)))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(userTenantRepository.findByUserIdAndIsPrimary(any(), any())).thenReturn(Optional.of(userTenant));
    when(usersClient.getUsersByUserId(any())).thenThrow(FeignException.class);
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getInstance()).thenReturn(folioExecutionContext);
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);

    assertThrows(java.lang.IllegalStateException.class,
      () -> userTenantService.save(UUID.fromString(CONSORTIUM_ID), tenant));
  }

  @Test
  void checkUserIfHasPrimaryAffiliationByUserIdSuccess() {
    var utEntity = createUserTenantEntity(UUID.randomUUID(), UUID.randomUUID(), "username", "diku");

    doNothing().when(consortiumService).checkConsortiumExistsOrThrow(any());
    when(conversionService.convert(any(), any())).thenReturn(toDto(utEntity));
    when(userTenantRepository.findByUserIdAndIsPrimary(any(), eq(true))).thenReturn(Optional.of(utEntity));

    var result = userTenantService.checkUserIfHasPrimaryAffiliationByUserId(UUID.randomUUID(), String.valueOf(UUID.randomUUID()));

    assertTrue(result);
  }


  private UserTenantEntity createUserTenantEntity(UUID associationId, UUID userId, String username, String tenantId) {
    UserTenantEntity userTenantEntity = new UserTenantEntity();
    userTenantEntity.setId(associationId);
    userTenantEntity.setUserId(userId);

    var tenant = new TenantEntity();
    tenant.setId(tenantId);
    tenant.setName("testtenant");
    tenant.setConsortiumId(UUID.fromString(CONSORTIUM_ID));
    userTenantEntity.setTenant(tenant);
    userTenantEntity.setUsername(username);
    return userTenantEntity;
  }

  private UserTenant toDto(UserTenantEntity userTenantEntity) {
    UserTenantConverter tenantConverter = new UserTenantConverter();
    return tenantConverter.convert(userTenantEntity);
  }

  private ConsortiumEntity createConsortiumEntity() {
    ConsortiumEntity consortiumEntity = new ConsortiumEntity();
    consortiumEntity.setId(UUID.fromString(CONSORTIUM_ID));
    consortiumEntity.setName("TestConsortium");
    return consortiumEntity;
  }

  private UserTenant createUserTenantDtoEntity() {
    UserTenant tenant = new UserTenant();
    tenant.setTenantId("diku");
    tenant.setTenantName("diku");
    tenant.setUserId(UUID.randomUUID());
    tenant.setId(UUID.randomUUID());
    tenant.setUsername("Test");
    tenant.setIsPrimary(false);

    return tenant;
  }

  private User createUserEntity(Boolean updateble) {
    User user = new User();
    Personal personal = new Personal();
    personal.setPreferredContactTypeId("email");
    personal.setEmail("Test@mail.com");
    personal.setFirstName("testFirst");
    personal.setLastName("testLast");
    user.setId(UUID.randomUUID().toString());
    user.setPatronGroup(null);
    user.setUsername("xyz");
    user.setPersonal(personal);
    user.setActive(Boolean.FALSE.equals(updateble));

    return user;
  }

  private UserEvent createUserEvent() {
    var userEvent = new UserEvent();
    userEvent.userDto(new Userdata()
      .id(UUID.randomUUID().toString())
      .username("userName"));
    return userEvent;
  }

  private User createNullUserEntity() {
    User user = new User();
    user.setId(UUID.randomUUID().toString());
    return user;
  }
}
