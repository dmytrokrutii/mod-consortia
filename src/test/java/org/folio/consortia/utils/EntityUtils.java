package org.folio.consortia.utils;

import lombok.experimental.UtilityClass;
import org.folio.consortia.domain.dto.ConsortiaConfiguration;
import org.folio.consortia.domain.dto.Consortium;
import org.folio.consortia.domain.dto.PublicationStatus;
import org.folio.consortia.domain.dto.SharingInstance;
import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.domain.dto.UserTenant;
import org.folio.consortia.domain.entity.ConsortiaConfigurationEntity;
import org.folio.consortia.domain.entity.ConsortiumEntity;
import org.folio.consortia.domain.entity.PublicationStatusEntity;
import org.folio.consortia.domain.entity.PublicationTenantRequestEntity;
import org.folio.consortia.domain.entity.SharingInstanceEntity;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.domain.entity.UserTenantEntity;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class EntityUtils {
  public static final UUID CONSORTIUM_ID = UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002");
  public static final UUID ACTION_ID = UUID.fromString("dcfc317b-0d7c-4334-8656-596105fa6c99");
  public static final UUID INSTANCE_ID = UUID.fromString("111841e3-e6fb-4191-8fd8-5674a5107c33");

  public static ConsortiumEntity createConsortiumEntity(String id, String name) {
    ConsortiumEntity consortiumEntity = new ConsortiumEntity();
    consortiumEntity.setId(UUID.fromString(id));
    consortiumEntity.setName(name);
    return consortiumEntity;
  }

  public static Consortium createConsortium(String id, String name) {
    Consortium consortium = new Consortium();
    consortium.setId(UUID.fromString(id));
    consortium.setName(name);
    return consortium;
  }

  public static TenantEntity createTenantEntity(String id, String name, String code, Boolean isCentral) {
    TenantEntity tenantEntity = new TenantEntity();
    tenantEntity.setId(id);
    tenantEntity.setCode(code);
    tenantEntity.setName(name);
    tenantEntity.setIsCentral(isCentral);
    tenantEntity.setConsortiumId(UUID.randomUUID());
    return tenantEntity;
  }

  public static TenantEntity createTenantEntity() {
    TenantEntity tenantEntity = new TenantEntity();
    tenantEntity.setId("testtenant1");
    tenantEntity.setCode("ABC");
    tenantEntity.setName("testtenant1");
    tenantEntity.setIsCentral(false);
    tenantEntity.setConsortiumId(UUID.randomUUID());
    return tenantEntity;
  }

  public static TenantEntity createTenantEntity(String id, String name) {
    TenantEntity tenantEntity = new TenantEntity();
    tenantEntity.setId(id);
    tenantEntity.setCode("ABC");
    tenantEntity.setName(name);
    tenantEntity.setIsCentral(false);
    return tenantEntity;
  }

  public static Tenant createTenant(String id, String name) {
    Tenant tenant = new Tenant();
    tenant.setId(id);
    tenant.setName(name);
    tenant.setIsCentral(false);
    tenant.setCode("ABC");
    return tenant;
  }

  public static Tenant createTenant(String id, String name, boolean isCentral) {
    Tenant tenant = new Tenant();
    tenant.setId(id);
    tenant.setName(name);
    tenant.setIsCentral(isCentral);
    tenant.setCode("ABC");
    return tenant;
  }

  public static UserTenant createUserTenant(UUID associationId) {
    UserTenant userTenant = new UserTenant();
    userTenant.setId(associationId);
    userTenant.setUserId(UUID.randomUUID());
    userTenant.setUsername("username");
    userTenant.setTenantId(String.valueOf(UUID.randomUUID()));
    userTenant.setIsPrimary(true);
    return userTenant;
  }

  public static UserTenantEntity createUserTenantEntity(UUID associationId) {
    UserTenantEntity userTenantEntity = new UserTenantEntity();
    userTenantEntity.setId(associationId);
    userTenantEntity.setTenant(new TenantEntity());
    userTenantEntity.setUsername("username");
    userTenantEntity.setUserId(UUID.randomUUID());
    userTenantEntity.setIsPrimary(false);
    return userTenantEntity;
  }

  public static ConsortiaConfigurationEntity createConsortiaConfigurationEntity(String centralTenantId) {
    ConsortiaConfigurationEntity configuration = new ConsortiaConfigurationEntity();
    configuration.setId(UUID.randomUUID());
    configuration.setCentralTenantId(centralTenantId);
    return configuration;
  }

  public static ConsortiaConfiguration createConsortiaConfiguration(String centralTenantId) {
    ConsortiaConfiguration configuration = new ConsortiaConfiguration();
    configuration.setId(UUID.randomUUID());
    configuration.setCentralTenantId(centralTenantId);
    return configuration;
  }

  public static SharingInstance createSharingInstance(UUID instanceIdentifier, String sourceTenantId, String targetTenantId) {
    SharingInstance sharingInstance = new SharingInstance();
    sharingInstance.setId(ACTION_ID);
    sharingInstance.setInstanceIdentifier(instanceIdentifier);
    sharingInstance.setSourceTenantId(sourceTenantId);
    sharingInstance.setTargetTenantId(targetTenantId);
    return sharingInstance;
  }

  public static SharingInstance createSharingInstance(UUID actionId, UUID instanceIdentifier, String sourceTenantId,
    String targetTenantId) {
    SharingInstance sharingInstance = new SharingInstance();
    sharingInstance.setId(actionId);
    sharingInstance.setInstanceIdentifier(instanceIdentifier);
    sharingInstance.setSourceTenantId(sourceTenantId);
    sharingInstance.setTargetTenantId(targetTenantId);
    return sharingInstance;
  }

  public static SharingInstanceEntity createSharingInstanceEntity(UUID instanceIdentifier, String sourceTenantId, String targetTenantId) {
    SharingInstanceEntity sharingInstance = new SharingInstanceEntity();
    sharingInstance.setId(ACTION_ID);
    sharingInstance.setInstanceId(instanceIdentifier);
    sharingInstance.setSourceTenantId(sourceTenantId);
    sharingInstance.setTargetTenantId(targetTenantId);
    sharingInstance.setCreatedDate(LocalDateTime.now());
    sharingInstance.setCreatedBy(UUID.fromString("dcfc317b-0d7c-4334-8656-596105fa6c99"));
    return sharingInstance;
  }

  public static SharingInstanceEntity createSharingInstanceEntity(UUID actionId, UUID instanceIdentifier, String sourceTenantId, String targetTenantId) {
    SharingInstanceEntity sharingInstance = new SharingInstanceEntity();
    sharingInstance.setId(actionId);
    sharingInstance.setInstanceId(instanceIdentifier);
    sharingInstance.setSourceTenantId(sourceTenantId);
    sharingInstance.setTargetTenantId(targetTenantId);
    sharingInstance.setCreatedDate(LocalDateTime.now());
    sharingInstance.setCreatedBy(UUID.fromString("dcfc317b-0d7c-4334-8656-596105fa6c99"));
    return sharingInstance;
  }

  public static PublicationTenantRequestEntity createPublicationTenantRequestEntity(PublicationStatusEntity publicationStatusEntity,
      String tenant, PublicationStatus status, int statusCode) {
    PublicationTenantRequestEntity entity = new PublicationTenantRequestEntity();
    entity.setId(UUID.randomUUID());
    entity.setTenantId(tenant);
    entity.setStatus(status);
    entity.setPcState(publicationStatusEntity);
    entity.setResponse(RandomStringUtils.random(10));
    entity.setRequestPayload(RandomStringUtils.random(10));
    entity.setResponseStatusCode(statusCode);
    entity.setCreatedDate(LocalDateTime.now());
    return entity;
  }
}
