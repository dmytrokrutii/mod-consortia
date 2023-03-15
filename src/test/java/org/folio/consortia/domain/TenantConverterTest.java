package org.folio.consortia.domain;

import org.folio.consortia.domain.converter.TenantConverter;
import org.folio.consortia.domain.entity.TenantEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantConverterTest {

  private final TenantConverter tenantConverter = new TenantConverter();

  @Test
  void shouldConvertEntityToDTO() {
    TenantEntity entity = new TenantEntity();
    entity.setId("id");
    entity.setName("name");
    var tenant = tenantConverter.convert(entity);
    assertEquals("id", tenant.getId());
    assertEquals("name", tenant.getName());
  }
}
