package org.folio.consortia.utils;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.messaging.MessageHeaders;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TenantContextUtils {
  public static FolioExecutionContext getFolioExecutionContextCopyForTenant(FolioExecutionContext context,
    String tenant) {
    var headers = context.getAllHeaders() != null
      ? context.getAllHeaders()
      : new HashMap<String, Collection<String>>();
    headers.put(XOkapiHeaders.TENANT, Collections.singletonList(tenant));

    return new DefaultFolioExecutionContext(context.getFolioModuleMetadata(), headers);
  }
  public static FolioExecutionContext getFolioExecutionContextCreatePrimaryAffiliationEvent(
                                                                                  MessageHeaders headers,
                                                                                  FolioModuleMetadata moduleMetadata) {
    return getContextFromKafkaHeaders(headers, moduleMetadata);
  }

  public static FolioExecutionContext getFolioExecutionContextDeletePrimaryAffiliationEvent(
                                                                                  MessageHeaders headers,
                                                                                  FolioModuleMetadata moduleMetadata) {
    return getContextFromKafkaHeaders(headers, moduleMetadata);
  }

  public static void runInFolioContext(FolioExecutionContext context, Runnable runnable) {
    try (var fec = new FolioExecutionContextSetter(context)) {
      runnable.run();
    }
  }

  private static FolioExecutionContext getContextFromKafkaHeaders(MessageHeaders headers,
                                                                  FolioModuleMetadata moduleMetadata) {
    Map<String, Collection<String>> map = new HashMap<>();
    map.put(XOkapiHeaders.TENANT, getHeaderValue(headers, XOkapiHeaders.TENANT, null));
    map.put(XOkapiHeaders.URL, getHeaderValue(headers, XOkapiHeaders.URL, null));
    map.put(XOkapiHeaders.TOKEN, getHeaderValue(headers, XOkapiHeaders.TOKEN, null));
    map.put(XOkapiHeaders.USER_ID, getHeaderValue(headers, XOkapiHeaders.USER_ID, null));

    return new DefaultFolioExecutionContext(moduleMetadata, map);
  }

  private static List<String> getHeaderValue(MessageHeaders headers, String headerName, String defaultValue) {
    var headerValue = headers.get(headerName);
    var value = headerValue == null
      ? defaultValue
      : new String((byte[]) headerValue, StandardCharsets.UTF_8);
    return value == null ? Collections.emptyList() : Collections.singletonList(value);
  }
}
