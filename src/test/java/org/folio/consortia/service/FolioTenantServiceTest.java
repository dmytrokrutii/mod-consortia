package org.folio.consortia.service;

import static org.folio.consortia.utils.EntityUtils.createOkapiHeaders;
import static org.folio.consortia.utils.EntityUtils.createUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.consortia.config.FolioExecutionContextHelper;
import org.folio.consortia.config.kafka.KafkaService;
import org.folio.consortia.domain.dto.CustomField;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.PermissionsClient;
import org.folio.spring.model.ResultList;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class FolioTenantServiceTest {
  private final String REQUESTING_TENANT = "diku";

  @InjectMocks
  FolioTenantService folioTenantService;
  @Mock
  KafkaService kafkaService;
  @Mock
  FolioExecutionContextHelper contextHelper;
  @Mock
  CustomFieldService customFieldService;
  @Mock
  PermissionsClient permissionsClient;
  @Mock
  FolioExecutionContext folioExecutionContext;
  @Mock
  ConsortiaConfigurationService consortiaConfigurationService;
  @Mock
  UserService userService;

  @Test
  void shouldUpdatePermissionList() {
    var sampleCustomField = CustomField.builder().build();
    var tenantAttributes = new TenantAttributes();
    UUID centralSystemUserId = UUID.randomUUID();
    String centralTenantId = "centralTenant";

    String systemUsername = "consortia-system-user";
    var centralSystemUser = Optional.of(createUser(centralSystemUserId, systemUsername));
    var shadowCentralSystemUser = createUser(centralSystemUserId, "shadow-system-user-1");

    List<String> shadowUserPermissionList = new ArrayList<>();
    shadowUserPermissionList.add("consortia.all");
    shadowUserPermissionList.add("users.all");
    ResultList<String> shadowSystemUserPermissionResultList = ResultList.of(2, shadowUserPermissionList);

    List<String> centralUserPermissionList = new ArrayList<>();
    centralUserPermissionList.add("consortia.all");
    centralUserPermissionList.add("users.all");
    centralUserPermissionList.add("invoices.all");
    ResultList<String> systemUserPermissionResultList = ResultList.of(3, centralUserPermissionList);

    mockOkapiHeaders();
    doNothing().when(contextHelper).registerTenant();
    doNothing().when(kafkaService).createKafkaTopics();
    doReturn(sampleCustomField).when(customFieldService).getCustomFieldByName("originalTenantId");

    doReturn(centralTenantId).when(consortiaConfigurationService).getCentralTenantId(REQUESTING_TENANT);
    doReturn(true).when(consortiaConfigurationService).isCentralTenantConfigurationExists();
    doReturn(centralSystemUser).when(userService).getByUsername(any());
    doReturn(shadowCentralSystemUser).when(userService).getById(centralSystemUserId);
    doNothing().when(permissionsClient).addPermission(shadowCentralSystemUser.getId(), new PermissionsClient.Permission("invoices.all"));
    when(permissionsClient.getUserPermissions(centralSystemUserId.toString()))
      .thenReturn(systemUserPermissionResultList)
      .thenReturn(shadowSystemUserPermissionResultList);

    // invoking method
    folioTenantService.afterTenantUpdate(tenantAttributes);

    // verifying
    verify(consortiaConfigurationService).getCentralTenantId(REQUESTING_TENANT);
    verify(consortiaConfigurationService).isCentralTenantConfigurationExists();
    verify(permissionsClient, times(2)).getUserPermissions(centralSystemUserId.toString());
    verify(userService).getById(centralSystemUserId);
    verify(userService).getByUsername(any());
    verify(permissionsClient).addPermission(shadowCentralSystemUser.getId(), new PermissionsClient.Permission("invoices.all"));

  }

  @Test
  void shouldNotDoAnyActionRequestingTenantIsCentralTenant() {
    var sampleCustomField = CustomField.builder().build();
    var tenantAttributes = new TenantAttributes();

    mockOkapiHeaders();
    doNothing().when(contextHelper).registerTenant();
    doNothing().when(kafkaService).createKafkaTopics();
    doReturn(sampleCustomField).when(customFieldService).getCustomFieldByName("originalTenantId");
    doReturn(REQUESTING_TENANT).when(consortiaConfigurationService).getCentralTenantId(REQUESTING_TENANT);
    doReturn(true).when(consortiaConfigurationService).isCentralTenantConfigurationExists();

    // invoking method
    folioTenantService.afterTenantUpdate(tenantAttributes);

    // verifying
    verify(consortiaConfigurationService).getCentralTenantId(REQUESTING_TENANT);
    verify(consortiaConfigurationService).isCentralTenantConfigurationExists();
    verifyNoInteractions(permissionsClient);
    verifyNoInteractions(userService);
    verifyNoInteractions(userService);
    verifyNoInteractions(permissionsClient);
  }

  @Test
  void shouldNotDoAnyActionIfCentralTenantIsNotExitsAndCustomFieldExits() {
    var sampleCustomField = CustomField.builder().build();
    var tenantAttributes = new TenantAttributes();

    mockOkapiHeaders();
    doNothing().when(contextHelper).registerTenant();
    doNothing().when(kafkaService).createKafkaTopics();
    doReturn(sampleCustomField).when(customFieldService).getCustomFieldByName("originalTenantId");
    doReturn(false).when(consortiaConfigurationService).isCentralTenantConfigurationExists();

    // invoking method
    folioTenantService.afterTenantUpdate(tenantAttributes);

    // verifying
    verify(consortiaConfigurationService).isCentralTenantConfigurationExists();
    verifyNoInteractions(permissionsClient);
    verifyNoInteractions(userService);
    verifyNoInteractions(userService);
    verifyNoInteractions(permissionsClient);
    verify(customFieldService, times(0)).createCustomField(any());
  }

  private void mockOkapiHeaders() {
    when(contextHelper.getSystemUserFolioExecutionContext(anyString())).thenReturn(folioExecutionContext);
    when(folioExecutionContext.getTenantId()).thenReturn(REQUESTING_TENANT);
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(createOkapiHeaders());
  }

}
