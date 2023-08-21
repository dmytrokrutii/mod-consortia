package org.folio.consortia.service;

import java.sql.ResultSet;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.consortia.config.FolioExecutionContextHelper;
import org.folio.consortia.config.kafka.KafkaService;
import org.folio.consortia.domain.dto.CustomField;
import org.folio.consortia.domain.dto.CustomFieldType;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@Primary
public class FolioTenantService extends TenantService {

  private static final String EXIST_SQL = "SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?)";

  private final KafkaService kafkaService;
  private final CustomFieldService customFieldService;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioExecutionContextHelper contextHelper;

  private static final String ORIGINAL_TENANT_ID_NAME = "originalTenantId";
  private static final CustomField ORIGINAL_TENANT_ID_CUSTOM_FIELD = CustomField.builder()
    .name(ORIGINAL_TENANT_ID_NAME)
    .entityType("user")
    .helpText("Id of tenant where user created originally")
    .customFieldType(CustomFieldType.TEXTBOX_LONG)
    .visible(false)
    .build();

  public FolioTenantService(JdbcTemplate jdbcTemplate,
                            KafkaService kafkaService,
                            FolioExecutionContext context,
                            FolioSpringLiquibase folioSpringLiquibase,
                            CustomFieldService customFieldService,
                            FolioExecutionContext folioExecutionContext,
                            FolioExecutionContextHelper contextHelper) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.kafkaService = kafkaService;
    this.customFieldService = customFieldService;
    this.folioExecutionContext = folioExecutionContext;
    this.contextHelper = contextHelper;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    try {
      contextHelper.registerTenant();
      kafkaService.createKafkaTopics();
      createOriginalTenantIdCustomField();
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
