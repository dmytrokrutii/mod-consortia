package org.folio.consortia.service;

import org.folio.consortia.repository.TenantRepository;
import org.folio.consortia.repository.entity.Tenant;
import org.folio.consortia.service.impl.TenantServiceImpl;
import org.folio.pv.domain.dto.TenantCollection;
import org.folio.spring.data.OffsetRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@EnableAutoConfiguration(exclude = BatchAutoConfiguration.class)
@EntityScan(basePackageClasses = Tenant.class)
class TenantServiceTest {
  @InjectMocks
  private TenantServiceImpl tenantService;
  @Mock
  private TenantRepository repository;

  @Test
  void shouldGetTenantList() {
    Tenant tenant1 = new Tenant();
    tenant1.setId("ABC1");
    tenant1.setName("TestName1");

    Tenant tenant2 = new Tenant();
    tenant1.setId("ABC2");
    tenant1.setName("TestName2");
    List<Tenant> tenantList = new ArrayList<>();
    tenantList.add(tenant1);
    tenantList.add(tenant2);
    Mockito.when(repository.findAll(new OffsetRequest(0,2)))
      .thenReturn(new PageImpl<>(tenantList) { });

    TenantCollection tenantCollection = tenantService.get(0, 2);
    Assertions.assertEquals(2, tenantCollection.getTotalRecords());
  }

  @Test
  void shouldGetError() {
    Assertions.assertThrows(IllegalArgumentException.class,() -> tenantService.get(0, 0));
  }
}
