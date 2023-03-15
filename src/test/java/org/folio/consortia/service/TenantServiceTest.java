package org.folio.consortia.service;

import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.domain.repository.TenantRepository;
import org.folio.consortia.service.impl.TenantServiceImpl;
import org.folio.spring.data.OffsetRequest;
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

import java.util.ArrayList;
import java.util.List;

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

  @Test
  void shouldGetTenantList() {

    TenantEntity tenantEntity1 = new TenantEntity();
    tenantEntity1.setId("ABC1");
    tenantEntity1.setName("TestName1");

    TenantEntity tenantEntity2 = new TenantEntity();
    tenantEntity1.setId("ABC1");
    tenantEntity1.setName("TestName1");
    List<TenantEntity> tenantEntityList = new ArrayList<>();
    tenantEntityList.add(tenantEntity1);
    tenantEntityList.add(tenantEntity2);
    when(repository.findAll(new OffsetRequest(0, 2)))
      .thenReturn(new PageImpl<>(tenantEntityList) {
      });

    var tenantCollection = tenantService.get(0, 2);
    Assertions.assertNotEquals(tenantEntity1, tenantEntity2);
    Assertions.assertEquals(2, tenantCollection.getTotalRecords());
  }

  @Test
  void shouldGetError() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> tenantService.get(0, 0));
  }
}
