package org.folio.consortia.service;

import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.domain.entity.ConsortiumEntity;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.repository.ConsortiumRepository;
import org.folio.consortia.repository.TenantRepository;
import org.folio.consortia.repository.UserTenantRepository;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.folio.consortia.service.impl.TenantServiceImpl;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnableAutoConfiguration(exclude = BatchAutoConfiguration.class)
@EntityScan(basePackageClasses = TenantEntity.class)
class TenantServiceTest {

  @InjectMocks
  private TenantServiceImpl tenantService;
  @Mock
  private TenantRepository tenantRepository;
  @Mock
  private UserTenantRepository userTenantRepository;
  @Mock
  private ConversionService conversionService;
  @Mock
  private ConsortiumRepository consortiumRepository;
  @Mock
  private ConsortiumService consortiumService;

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
    when(tenantRepository.findByConsortiumId(any(), any(PageRequest.of(offset, limit).getClass()))).thenReturn(new PageImpl<>(tenantEntityList, PageRequest.of(offset, limit), tenantEntityList.size()));

    var tenantCollection = tenantService.get(consortiumId, 0, 10);
    Assertions.assertEquals(2, tenantCollection.getTotalRecords());
  }

  @Test
  void shouldSaveTenant() {
    UUID consortiumId = UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002");
    TenantEntity tenantEntity1 = createTenantEntity("ABC1", "TestName1");
    Tenant tenant = createTenant("TestID", "Test");

    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    when(tenantRepository.existsById(any())).thenReturn(false);
    when(tenantRepository.save(any(TenantEntity.class))).thenReturn(tenantEntity1);
    when(conversionService.convert(tenantEntity1, Tenant.class)).thenReturn(tenant);

    var tenant1 = tenantService.save(consortiumId, tenant);
    Assertions.assertEquals(tenant, tenant1);
  }

  @Test
  void shouldUpdateTenant() {
    UUID consortiumId = UUID.randomUUID();
    TenantEntity tenantEntity1 = createTenantEntity("TestID", "TestName1");
    Tenant tenant = createTenant("TestID", "TestName2");

    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    when(tenantRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.save(any(TenantEntity.class))).thenReturn(tenantEntity1);
    when(conversionService.convert(tenantEntity1, Tenant.class)).thenReturn(tenant);

    var tenant1 = tenantService.update(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), tenant.getId(), tenant);
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
    assertThrows(IllegalArgumentException.class, () -> tenantService.delete(consortiumId, tenantId));
  }

  @Test
  void testDeleteNonexistentTenant() {
    UUID consortiumId = UUID.randomUUID();
    String tenantId = "123";

    // Mock repository method calls
    when(tenantRepository.existsById(tenantId)).thenReturn(false);

    // Call the method
    assertThrows(ResourceNotFoundException.class, () -> tenantService.delete(consortiumId, tenantId));
  }

  @Test
  void shouldThrowExceptionWhileUpdateTenant() {
    TenantEntity tenantEntity1 = createTenantEntity("TestID", "TestName1");
    Tenant tenant = createTenant("TestID", "TestName2");

    when(consortiumRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.save(any(TenantEntity.class))).thenReturn(tenantEntity1);
    when(conversionService.convert(tenantEntity1, Tenant.class)).thenReturn(tenant);
    assertThrows(java.lang.IllegalArgumentException.class, () -> tenantService.update(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), tenant.getId() + "1234", tenant));
  }

  @Test
  void shouldThrowNotFoundExceptionWhileUpdateTenant() {
    TenantEntity tenantEntity1 = createTenantEntity("TestID", "TestName1");
    Tenant tenant = createTenant("TestID", "TestName2");

    when(consortiumRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.save(any(TenantEntity.class))).thenReturn(tenantEntity1);
    when(conversionService.convert(tenantEntity1, Tenant.class)).thenReturn(tenant);
    assertThrows(org.folio.consortia.exception.ResourceNotFoundException.class, () -> tenantService.update(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), tenant.getId() + "1234", tenant));
  }

  @Test
  void shouldNotSaveTenantForDuplicateId() {
    TenantEntity tenantEntity1 = createTenantEntity("TestID", "Test");
    Tenant tenant = createTenant("TestID", "Testq");

    when(tenantRepository.existsById(any())).thenReturn(true);
    when(conversionService.convert(tenantEntity1, Tenant.class)).thenReturn(tenant);

    assertThrows(org.folio.consortia.exception.ResourceAlreadyExistException.class, () -> tenantService.save(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), tenant));
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

  private ConsortiumEntity createConsortiumEntity() {
    ConsortiumEntity consortiumEntity = new ConsortiumEntity();
    consortiumEntity.setId(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"));
    consortiumEntity.setName("TestConsortium");
    return consortiumEntity;
  }

  private TenantEntity createTenantEntity(String id, String name) {
    TenantEntity tenantEntity = new TenantEntity();
    tenantEntity.setId(id);
    tenantEntity.setName(name);
    return tenantEntity;
  }

  private Tenant createTenant(String id, String name) {
    Tenant tenant = new Tenant();
    tenant.setId(id);
    tenant.setName(name);
    return tenant;
  }
}
