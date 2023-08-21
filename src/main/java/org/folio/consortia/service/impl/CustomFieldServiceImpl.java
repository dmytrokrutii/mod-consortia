package org.folio.consortia.service.impl;

import static java.lang.String.format;

import java.net.URI;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.consortia.client.CustomFieldsClient;
import org.folio.consortia.client.OkapiClient;
import org.folio.consortia.domain.dto.CustomField;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.folio.consortia.service.CustomFieldService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class CustomFieldServiceImpl implements CustomFieldService {
  private final FolioExecutionContext folioExecutionContext;

  private final OkapiClient okapiClient;
  private static final String OKAPI_URL = "http://_";
  private final CustomFieldsClient customFieldsClient;
  private static final String MOD_USERS = "mod-users";
  private static final String QUERY_PATTERN_NAME = "name==%s";

  @Override
  public void createCustomField(CustomField customField) {
    log.info("createCustomField::creating new custom-field with name {}", customField.getName());
    customFieldsClient.postCustomFields(getModuleId(MOD_USERS), customField);
  }

  public CustomField getCustomFieldByName(String name) {
    log.debug("getCustomFieldByName::getting custom-field with name {}.", name);
    return customFieldsClient.getByQuery(getModuleId(MOD_USERS), format(QUERY_PATTERN_NAME, name))
      .getCustomFields().stream().filter(customField -> customField.getName().equals(name))
      .findFirst()
      .orElse(null);
  }

  @Cacheable(cacheNames = "moduleIds")
  public String getModuleId(String moduleName) {
    var tenantId = folioExecutionContext.getTenantId();
    var moduleNamesJson = okapiClient.getModuleIds(URI.create(OKAPI_URL), tenantId, moduleName);
    if (!moduleNamesJson.isEmpty()) {
      return moduleNamesJson.get(0).get("id").asText();
    }
    throw new ResourceNotFoundException(format("Module id not found for name: %s", moduleName));
  }
}
