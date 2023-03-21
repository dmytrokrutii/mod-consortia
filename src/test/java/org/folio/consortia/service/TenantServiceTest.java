package org.folio.consortia.service;

import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.domain.entity.ConsortiumEntity;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.domain.repository.ConsortiumRepository;
import org.folio.consortia.domain.repository.TenantRepository;
import org.folio.consortia.service.impl.TenantServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnableAutoConfiguration(exclude = BatchAutoConfiguration.class)
@EntityScan(basePackageClasses = TenantEntity.class)
class TenantServiceTest {

  @InjectMocks
  private TenantServiceImpl tenantService;

  @Mock
  private TenantRepository repository;

  @Mock
  private ConversionService conversionService;

  @Mock
  private ConsortiumRepository consortiumRepository;

  @Test
  void shouldGetTenantList() {
    int offset = 0;
    int limit = 2;
    TenantEntity tenantEntity1 = new TenantEntity();
    tenantEntity1.setId("ABC1");
    tenantEntity1.setName("TestName1");

    TenantEntity tenantEntity2 = new TenantEntity();
    tenantEntity1.setId("ABC1");
    tenantEntity1.setName("TestName1");
    List<TenantEntity> tenantEntityList = new ArrayList<>();
    tenantEntityList.add(tenantEntity1);
    tenantEntityList.add(tenantEntity2);
    ConsortiumEntity consortiumEntity = new ConsortiumEntity();
    consortiumEntity.setId(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"));
    consortiumEntity.setName("TestConsortium");

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(consortiumEntity));
    when(repository.findByConsortiumId(any(), any(PageRequest.of(offset, limit).getClass())))
      .thenReturn(new PageImpl<>(tenantEntityList, PageRequest.of(offset, limit), tenantEntityList.size()));

    var tenantCollection = tenantService.get(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"),0, 10);
    Assertions.assertNotEquals(tenantEntity1, tenantEntity2);
    Assertions.assertEquals(2, tenantCollection.getTotalRecords());
  }

  @Test
  void shouldGetError() {
    Assertions.assertThrows(org.folio.consortia.exception.ResourceNotFoundException.class, () -> tenantService.get(null,0, 0));
  }

  @Test
  void shouldSaveTenant() {
    TenantEntity tenantEntity1 = new TenantEntity();
    tenantEntity1.setId("ABC1");
    tenantEntity1.setName("TestName1");
    Tenant tenant = new Tenant();
    tenant.setId("TestID");
    tenant.setName("Test");

    ConsortiumEntity consortiumEntity = new ConsortiumEntity();
    consortiumEntity.setId(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"));
    consortiumEntity.setName("TestConsortium");

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"))).thenReturn(Optional.of(consortiumEntity));
    when(repository.save(any(TenantEntity.class))).thenReturn(tenantEntity1);
    when(conversionService.convert(tenant, TenantEntity.class)).thenReturn(tenantEntity1);
    when(conversionService.convert(tenantEntity1, Tenant.class)).thenReturn(tenant);

    var tenant1 = tenantService.save(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), tenant);
    Assertions.assertEquals(tenant, tenant1);
  }

  @Test
  void shouldNotSaveTenantForDuplicateId() {
    TenantEntity tenantEntity1 = new TenantEntity();
    tenantEntity1.setId("TestID");
    tenantEntity1.setName("Test");
    Tenant tenant = new Tenant();
    tenant.setId("TestID");
    tenant.setName("Testq");

    ConsortiumEntity consortiumEntity = new ConsortiumEntity();
    consortiumEntity.setId(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"));
    consortiumEntity.setName("TestConsortium");

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"))).thenReturn(Optional.of(consortiumEntity));

    when(repository.findById(tenant.getId())).thenReturn(Optional.of(tenantEntity1));
    when(conversionService.convert(tenant, TenantEntity.class)).thenReturn(tenantEntity1);
    when(conversionService.convert(tenantEntity1, Tenant.class)).thenReturn(tenant);

    Assertions.assertThrows(org.folio.consortia.exception.ResourceAlreadyExistException.class,
      () -> tenantService.save(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), tenant));
  }
}
