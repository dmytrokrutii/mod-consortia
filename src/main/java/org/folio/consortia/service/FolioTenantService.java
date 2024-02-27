package org.folio.consortia.service;

import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.consortia.config.FolioExecutionContextHelper;
import org.folio.consortia.config.kafka.KafkaService;
import org.folio.consortia.domain.dto.CustomField;
import org.folio.consortia.domain.dto.CustomFieldType;
import org.folio.consortia.domain.dto.User;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.PermissionsClient;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@Primary
public class FolioTenantService extends TenantService {

  private static final String EXIST_SQL = "SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?)";

  private final KafkaService kafkaService;
  private final CustomFieldService customFieldService;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioExecutionContextHelper contextHelper;
  private final UserService userService;
  private final PermissionsClient permissionsClient;
  private final ConsortiaConfigurationService consortiaConfigurationService;

  @Value("${folio.system-user.username}")
  private String systemUserUsername;

  private static final String ORIGINAL_TENANT_ID_NAME = "originalTenantId";
  private static final CustomField ORIGINAL_TENANT_ID_CUSTOM_FIELD = CustomField.builder()
    .name(ORIGINAL_TENANT_ID_NAME)
    .entityType("user")
    .helpText("Id of tenant where user created originally")
    .customFieldType(CustomFieldType.TEXTBOX_LONG)
    .visible(false)
    .build();

  public FolioTenantService(JdbcTemplate jdbcTemplate, KafkaService kafkaService, FolioExecutionContext context,
                            FolioSpringLiquibase folioSpringLiquibase, CustomFieldService customFieldService,
                            FolioExecutionContext folioExecutionContext, FolioExecutionContextHelper contextHelper,
                            UserService userService, PermissionsClient permissionsClient, ConsortiaConfigurationService consortiaConfigurationService) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.kafkaService = kafkaService;
    this.customFieldService = customFieldService;
    this.folioExecutionContext = folioExecutionContext;
    this.contextHelper = contextHelper;
    this.userService = userService;
    this.permissionsClient = permissionsClient;
    this.consortiaConfigurationService = consortiaConfigurationService;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    try {
      contextHelper.registerTenant();
      kafkaService.createKafkaTopics();
      createOriginalTenantIdCustomField();
      updateLocalTenantShadowSystemUsers();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Implemented by HSQLDB way
   * Check if the tenant exists (by way of its database schema)
   * @return if the tenant's database schema exists
   */
  @Override
  protected boolean tenantExists() {
    return BooleanUtils.isTrue(
      jdbcTemplate.query(EXIST_SQL,
        (ResultSet resultSet) -> resultSet.next() && resultSet.getBoolean(1),
        getSchemaName()
      )
    );
  }

  /**
   * In second time installing of module, method looks for system user permission changes
   * if there is new changes in system user of central tenant,
   * permission of shadow users will be updated with same ones with central system user in requesting tenant.
   * This functionality is required by sharing settings because operation from central tenant context use system users,
   * so each shadow users permissions also should be updated
   */
  private void updateLocalTenantShadowSystemUsers() {
    if (!consortiaConfigurationService.isCentralTenantConfigurationExists()) {
      log.info("The first time module is being installed, so skipping phase of updating shadow user permission");
      return;
    }

    log.debug("updateLocalTenantShadowSystemUsers:: Trying to update shadow user permissions");
    String requestingTenant = folioExecutionContext.getTenantId();
    String centralTenantId = consortiaConfigurationService.getCentralTenantId(requestingTenant);

    if (Objects.equals(requestingTenant, centralTenantId)) {
      log.info("updateLocalTenantShadowSystemUsers:: consortia module is being installed on central tenant, no action required");
      return;
    }

    // Fetch system user and its permission lists from central tenant
    User centralSystemUser;
    Set<String> systemUserPermissionList;
    try (var ignored = new FolioExecutionContextSetter(contextHelper.getSystemUserFolioExecutionContext(centralTenantId))) {
      centralSystemUser = userService.getByUsername(systemUserUsername)
        .orElseThrow(() -> new ResourceNotFoundException("systemUserUsername", systemUserUsername));
      systemUserPermissionList = new HashSet<>(permissionsClient.getUserPermissions(centralSystemUser.getId()).getResult());
      log.info("updateLocalTenantShadowSystemUsers:: Retrieved permissions '{}' of centralSystemUser '{}' from centralTenantId={}",
        systemUserPermissionList, centralSystemUser.getId(), centralTenantId);
    }

    try (var ignored = new FolioExecutionContextSetter(contextHelper.getSystemUserFolioExecutionContext(requestingTenant))) {
      var shadowCentralSystemUser = userService.getById(UUID.fromString(centralSystemUser.getId()));
      String shadowSystemUserId = shadowCentralSystemUser.getId();
      Set<String> shadowSystemUserPermissionList = new HashSet<>(permissionsClient.getUserPermissions(shadowSystemUserId).getResult());
      log.info("updateLocalTenantShadowSystemUsers:: Retrieved permissions '{}' of shadowSystemUser '{}' from requestingTenant={}",
        shadowSystemUserPermissionList, shadowSystemUserId, requestingTenant);

      shadowSystemUserPermissionList.forEach(systemUserPermissionList::remove);
      systemUserPermissionList.forEach(permission ->
        permissionsClient.addPermission(shadowSystemUserId, new PermissionsClient.Permission(permission))
      );
      log.info("updateLocalTenantShadowSystemUsers:: Permissions assigned to shadow system user: [{}] of tenant: {}", systemUserPermissionList, requestingTenant);
    }
  }

  private void createOriginalTenantIdCustomField() {
    try (var ignored = new FolioExecutionContextSetter(contextHelper.getSystemUserFolioExecutionContext(folioExecutionContext.getTenantId()))) {
      if (ObjectUtils.isNotEmpty(customFieldService.getCustomFieldByName(ORIGINAL_TENANT_ID_NAME))) {
        log.info("createOriginalTenantIdCustomField:: custom-field already available in tenant {} with name {}", folioExecutionContext.getTenantId(), ORIGINAL_TENANT_ID_NAME);
      } else {
        customFieldService.createCustomField(ORIGINAL_TENANT_ID_CUSTOM_FIELD);
      }
    }
  }
}
