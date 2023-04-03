package org.folio.consortia.service;

import org.folio.consortia.domain.converter.UserTenantConverter;
import org.folio.consortia.domain.dto.UserTenant;
import org.folio.consortia.domain.dto.UserTenantCollection;
import org.folio.consortia.domain.entity.ConsortiumEntity;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.domain.entity.UserTenantEntity;
import org.folio.consortia.repository.ConsortiumRepository;
import org.folio.consortia.repository.UserTenantRepository;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.folio.consortia.service.impl.UserTenantServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnableAutoConfiguration(exclude = BatchAutoConfiguration.class)
@EntityScan(basePackageClasses = UserTenantEntity.class)
class UserTenantServiceTest {

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

  @Test
  void shouldGetUserTenantList() {
    // given
    List<UserTenantEntity> userTenantEntities = List.of(new UserTenantEntity(), new UserTenantEntity());
    Page<UserTenantEntity> userTenantPage = new PageImpl<>(userTenantEntities, PageRequest.of(0, 10), userTenantEntities.size());

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(userTenantRepository.findAll(PageRequest.of(0, 10))).thenReturn(userTenantPage);

    // when
    var result = userTenantService.get(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), 0, 10);

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

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(conversionService.convert(userTenant, UserTenant.class)).thenReturn(toDto(userTenant));
    when(userTenantRepository.findById(associationId)).thenReturn(Optional.of(userTenantEntities.get(0)));

    // when
    var result = userTenantService.getById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), associationId);

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

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(conversionService.convert(userTenant, UserTenant.class)).thenReturn(toDto(userTenant));
    when(conversionService.convert(userTenant2, UserTenant.class)).thenReturn(toDto(userTenant2));
    when(userTenantRepository.findByUserId(userId, PageRequest.of(0, 10)))
      .thenReturn(new PageImpl<>(userTenantEntities, PageRequest.of(0, 10), userTenantEntities.size()));

    // when
    UserTenantCollection result = userTenantService.getByUserId(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), userId, 0, 10);

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

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(conversionService.convert(userTenant, UserTenant.class)).thenReturn(toDto(userTenant));
    when(userTenantRepository.findByUsernameAndTenantId("testuser", tenantId)).thenReturn(Optional.of(userTenant));

    // when
    UserTenantCollection result = userTenantService.getByUsernameAndTenantId(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), "testuser", tenantId);

    // then
    assertEquals(tenantId, result.getUserTenants().get(0).getTenantId());
    assertEquals(1, result.getTotalRecords());
  }

  @Test
  void shouldThrowIllegalArgumentException() {
    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(createConsortiumEntity()));
    UUID id = UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002");
    Assertions.assertThrows(IllegalArgumentException.class, () -> userTenantService.get(id, 0, 0));
  }

  @Test
  void shouldReturn404UserIdNotFoundException() {
    // given
    UUID userId = UUID.randomUUID();

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(userTenantRepository.findByUserId(userId, PageRequest.of(0, 10)))
      .thenReturn(new PageImpl<>(new ArrayList<>()));

    UUID id = UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002");
    // throw exception
    assertThrows(ResourceNotFoundException.class, () -> userTenantService.getByUserId(id, userId, 0, 10));
  }

  @Test
  void shouldReturn404UsernameNotFoundException() {
    // given
    String username = "testuser";
    String tenantId = String.valueOf(UUID.randomUUID());

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(createConsortiumEntity()));
    when(userTenantRepository.findByUsernameAndTenantId(username, tenantId))
      .thenReturn(Optional.empty());
    UUID id = UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002");

    // throw exception
    assertThrows(ResourceNotFoundException.class,
      () -> userTenantService.getByUsernameAndTenantId(id, "testusername", tenantId));
  }

  private UserTenantEntity createUserTenantEntity(UUID associationId, UUID userId, String username, String tenantId) {
    UserTenantEntity userTenantEntity = new UserTenantEntity();
    userTenantEntity.setId(associationId);
    userTenantEntity.setUserId(userId);

    var tenant = new TenantEntity();
    tenant.setId(tenantId);
    tenant.setName("testtenant");
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
    consortiumEntity.setId(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"));
    consortiumEntity.setName("TestConsortium");
    return consortiumEntity;
  }
}

