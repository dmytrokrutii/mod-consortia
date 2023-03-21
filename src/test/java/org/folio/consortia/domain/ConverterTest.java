package org.folio.consortia.domain;

import org.folio.consortia.domain.converter.TenantConverter;
import org.folio.consortia.domain.converter.UserTenantConverter;
import org.folio.consortia.domain.dto.UserTenant;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.domain.entity.UserTenantEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConverterTest {

  private final TenantConverter tenantConverter = new TenantConverter();
  private final UserTenantConverter converter = new UserTenantConverter();

  @Test
  void shouldConvertUserEntityToDTO() {
    UserTenantEntity entity = new UserTenantEntity();
    entity.setId(UUID.randomUUID());
    entity.setUserId(UUID.randomUUID());
    entity.setUsername("testuser");
    entity.setIsPrimary(true);

    var tenantEntity = new TenantEntity();
    tenantEntity.setId(UUID.randomUUID().toString());
    tenantEntity.setName("Test Tenant");

    entity.setTenant(tenantEntity);

    UserTenant dto = converter.convert(entity);

    assertEquals(entity.getId(), dto.getId());
    assertEquals(entity.getUserId(), dto.getUserId());
    assertEquals(entity.getUsername(), dto.getUsername());
    assertEquals(entity.getIsPrimary(), dto.getIsPrimary());
    assertEquals(entity.getTenant().getId(), dto.getTenantId());
    assertEquals(entity.getTenant().getName(), dto.getTenantName());
  }

  @Test
  void shouldConvertTenantEntityToDTO() {
    TenantEntity entity = new TenantEntity();
    entity.setId("id");
    entity.setName("name");
    var tenant = tenantConverter.convert(entity);
    assertEquals("id", tenant.getId());
    assertEquals("name", tenant.getName());
  }
}
