package org.folio.consortia.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.consortia.support.BaseIT;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FolioExecutionContextHelperTest extends BaseIT {

  @Autowired
  private FolioExecutionContextHelper contextHelper;

  @Test
  void shouldGetFolioExecutionContext() {
    FolioExecutionContext executionContext = contextHelper.getSystemUserFolioExecutionContext(TENANT);

    assertEquals(TENANT, executionContext.getTenantId());
    assertEquals(wireMockServer.baseUrl(), executionContext.getOkapiUrl());
    assertEquals("accessToken", executionContext.getToken());
  }
}
