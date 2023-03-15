package org.folio.consortia.service;

import org.folio.consortia.domain.converter.UserTenantConverter;
import org.folio.consortia.domain.dto.UserTenant;
import org.folio.consortia.domain.dto.UserTenantCollection;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.domain.entity.UserTenantEntity;
import org.folio.consortia.domain.repository.UserTenantRepository;
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

  @Test
  void shouldGetUserTenantList() {
    // given
    int offset = 0;
    int limit = 10;
    List<UserTenantEntity> userTenantEntities = List.of(new UserTenantEntity(), new UserTenantEntity());
    Page<UserTenantEntity> userTenantPage = new PageImpl<>(userTenantEntities, PageRequest.of(offset, limit), userTenantEntities.size());

    when(userTenantRepository.findAll(PageRequest.of(offset, limit))).thenReturn(userTenantPage);

    // when
    var result = userTenantService.get(offset, limit);

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

    when(conversionService.convert(userTenant, UserTenant.class)).thenReturn(toDto(userTenant));
    when(userTenantRepository.findById(associationId)).thenReturn(Optional.of(userTenantEntities.get(0)));

    // when
    var result = userTenantService.getById(associationId);

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
    int limit = 10;
    int offset = 0;

    UserTenantEntity userTenant = createUserTenantEntity(associationId, userId, "testuser", tenantId);
    UserTenantEntity userTenant2 = createUserTenantEntity(associationId, userId, "testuser", tenantId);
    List<UserTenantEntity> userTenantEntities = List.of(userTenant);

    when(conversionService.convert(userTenant, UserTenant.class)).thenReturn(toDto(userTenant));
    when(conversionService.convert(userTenant2, UserTenant.class)).thenReturn(toDto(userTenant2));
    when(userTenantRepository.findByUserId(userId, PageRequest.of(offset, limit)))
      .thenReturn(new PageImpl<>(userTenantEntities, PageRequest.of(offset, limit), userTenantEntities.size()));

    // when
    UserTenantCollection result = userTenantService.getByUserId(userId, offset, limit);

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

    when(conversionService.convert(userTenant, UserTenant.class)).thenReturn(toDto(userTenant));
    when(userTenantRepository.findByUsernameAndTenantId("testuser", tenantId)).thenReturn(Optional.of(userTenant));

    // when
    UserTenantCollection result = userTenantService.getByUsernameAndTenantId("testuser", tenantId);

    // then
    assertEquals(tenantId, result.getUserTenants().get(0).getTenantId());
    assertEquals(1, result.getTotalRecords());
  }

  @Test
  void shouldThrowIllegalArgumentException() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> userTenantService.get(0, 0));
  }

  @Test
  void shouldReturn404UserIdNotFoundException() {
    // given
    UUID userId = UUID.randomUUID();
    int limit = 10;
    int offset = 0;
    when(userTenantRepository.findByUserId(userId, PageRequest.of(offset, limit)))
      .thenReturn(new PageImpl<>(new ArrayList<>()));

    // throw exception
    assertThrows(ResourceNotFoundException.class, () -> userTenantService.getByUserId(userId, offset, limit));
  }

  @Test
  void shouldReturn404UsernameNotFoundException() {
    // given
    String username = "testuser";
    String tenantId = String.valueOf(UUID.randomUUID());
    when(userTenantRepository.findByUsernameAndTenantId(username, tenantId))
      .thenReturn(Optional.empty());

    // throw exception
    assertThrows(ResourceNotFoundException.class,
      () -> userTenantService.getByUsernameAndTenantId("testusername", tenantId));
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

}

