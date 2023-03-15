package org.folio.consortia.domain;

import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.domain.entity.UserTenantEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EntityTest {

  @Test
  void testEqualsAndHashCodeForTenantEntity() {
    TenantEntity tenant1 = new TenantEntity();
    tenant1.setId("ABC1");
    tenant1.setName("TestName1");

    TenantEntity tenant2 = new TenantEntity();
    tenant2.setId("ABC1");
    tenant2.setName("TestName1");

    TenantEntity tenant3 = new TenantEntity();
    tenant3.setId("XYZ1");
    tenant3.setName("TestName2");

    assertEquals(tenant1, tenant2);
    assertNotEquals(tenant1, tenant3);
    assertEquals(tenant1.hashCode(), tenant2.hashCode());
    assertNotEquals(tenant1.hashCode(), tenant3.hashCode());
  }


  @Test
  void testEqualsAndHashCodeForUserTenantEntity() {
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String username = "test_user";
    TenantEntity tenant = new TenantEntity();
    tenant.setId(String.valueOf(UUID.randomUUID()));
    Boolean isPrimary = true;

    UserTenantEntity userTenant1 = new UserTenantEntity();
    userTenant1.setId(id);
    userTenant1.setUserId(userId);
    userTenant1.setUsername(username);
    userTenant1.setTenant(tenant);
    userTenant1.setIsPrimary(isPrimary);

    UserTenantEntity userTenant2 = new UserTenantEntity();
    userTenant2.setId(id);
    userTenant2.setUserId(userId);
    userTenant2.setUsername(username);
    userTenant2.setTenant(tenant);
    userTenant2.setIsPrimary(isPrimary);

    assertEquals(userTenant1, userTenant2);
    assertEquals(userTenant1.hashCode(), userTenant2.hashCode());

    userTenant2.setIsPrimary(false);

    assertNotEquals(userTenant1, userTenant2);
    assertNotEquals(userTenant1.hashCode(), userTenant2.hashCode());
  }


}
