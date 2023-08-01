package org.folio.consortia.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.folio.consortia.security.AuthService;
import org.folio.consortia.security.SecurityManagerService;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class FolioExecutionContextHelper {

  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final AuthService authService;
  private final SecurityManagerService securityManagerService;

  @Value("${folio.okapi.url}")
  private String okapiUrl;

  public void registerTenant() {
    securityManagerService.prepareSystemUser(folioExecutionContext.getOkapiUrl(), folioExecutionContext.getTenantId());
  }

  public FolioExecutionContext getSystemUserFolioExecutionContext(String tenantId) {
    Map<String, Collection<String>> tenantOkapiHeaders = new HashMap<>();
    tenantOkapiHeaders.put(XOkapiHeaders.TENANT, List.of(tenantId));
    tenantOkapiHeaders.put(XOkapiHeaders.URL, List.of(okapiUrl));

    try (var context = new FolioExecutionContextSetter(new DefaultFolioExecutionContext(folioModuleMetadata, tenantOkapiHeaders))) {
      String systemUserToken = authService.getTokenForSystemUser(tenantId, okapiUrl);
      log.debug("getSystemUserFolioExecutionContext:: {}", systemUserToken);
      if (StringUtils.isNotBlank(systemUserToken)) {
        tenantOkapiHeaders.put(XOkapiHeaders.TOKEN, List.of(systemUserToken));
      } else {
        throw new IllegalStateException(String.format("Cannot create FolioExecutionContext for Tenant: %s because of absent token", tenantId));
      }
    }

    try (var context = new FolioExecutionContextSetter(new DefaultFolioExecutionContext(folioModuleMetadata, tenantOkapiHeaders))) {
      String systemUserId = authService.getSystemUserId();
      if (StringUtils.isNotEmpty(systemUserId)) {
        tenantOkapiHeaders.put(XOkapiHeaders.USER_ID, List.of(systemUserId));
      }
    }
    return new DefaultFolioExecutionContext(folioModuleMetadata, tenantOkapiHeaders);
  }
}
