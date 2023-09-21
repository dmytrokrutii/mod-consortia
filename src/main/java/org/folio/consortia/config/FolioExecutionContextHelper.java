package org.folio.consortia.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.context.ExecutionContextBuilder;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.SystemUserService;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class FolioExecutionContextHelper {

  private final ExecutionContextBuilder contextBuilder;
  private final PrepareSystemUserService prepareSystemUserService;
  private final SystemUserService systemUserService;

  public void registerTenant() {
    prepareSystemUserService.setupSystemUser();
  }

  public FolioExecutionContext getSystemUserFolioExecutionContext(String tenantId) {
    return contextBuilder.forSystemUser(systemUserService.getAuthedSystemUser(tenantId));
  }
}
