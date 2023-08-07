package org.folio.consortia.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.folio.consortia.utils.EntityUtils.createPublicationRequestForSetting;
import static org.folio.consortia.utils.EntityUtils.createSharingSettingResponse;
import static org.folio.consortia.utils.EntityUtils.createSharingSettingResponseForDelete;
import static org.folio.consortia.utils.EntityUtils.createTenant;
import static org.folio.consortia.utils.EntityUtils.createTenantCollection;
import static org.folio.consortia.utils.InputOutputTestUtils.getMockDataObject;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.folio.consortia.config.FolioExecutionContextHelper;
import org.folio.consortia.domain.dto.PublicationResponse;
import org.folio.consortia.domain.dto.SharingSettingRequest;
import org.folio.consortia.domain.dto.Tenant;
import org.folio.consortia.domain.dto.TenantCollection;
import org.folio.consortia.domain.entity.SharingSettingEntity;
import org.folio.consortia.repository.ConsortiumRepository;
import org.folio.consortia.repository.SharingSettingRepository;
import org.folio.consortia.service.impl.SharingSettingServiceImpl;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
class SharingSettingServiceTest {
  private static final String SHARING_SETTING_REQUEST_SAMPLE = "mockdata/sharing_setting_request.json";
  private static final String SHARING_SETTING_REQUEST_SAMPLE_WITHOUT_PAYLOAD = "mockdata/sharing_setting_request_without_payload.json";
  @InjectMocks
  private SharingSettingServiceImpl sharingSettingService;
  @Mock
  private ConsortiumRepository consortiumRepository;
  @Mock
  private ConsortiumService consortiumService;
  @Mock
  private TenantService tenantService;
  @Mock
  private PublicationService publicationService;
  @Mock
  private SharingSettingRepository sharingSettingRepository;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private FolioExecutionContextHelper contextHelper;
  @Mock
  private ObjectMapper objectMapper;

  @Test
  void shouldStartSharingSetting() throws JsonProcessingException {
    UUID consortiumId = UUID.randomUUID();
    UUID createSettingsPcId = UUID.randomUUID();
    UUID updateSettingsPcId = UUID.randomUUID();
    Tenant tenant1 = createTenant("tenant1", "tenant1");
    Tenant tenant2 = createTenant("tenant2", "tenant2");
    Set<String> tenantAssociationsWithSetting = Set.of("tenant1");
    TenantCollection tenantCollection = createTenantCollection(List.of(tenant1, tenant2));
    var sharingSettingRequest = getMockDataObject(SHARING_SETTING_REQUEST_SAMPLE, SharingSettingRequest.class);
    Map<String, String> payload = new LinkedHashMap<>();
    payload.put("id", "1844767a-8367-4926-9999-514c35840399");
    payload.put("name", "ORG-NAME");
    payload.put("source", "local");

    // "tenant1" exists in tenant setting association so that tenant1 is in PUT request publication,
    // "tenant2" is in POST method publication
    var publicationRequestPut = createPublicationRequestForSetting(sharingSettingRequest, HttpMethod.PUT.toString());
    publicationRequestPut.setMethod("PUT");
    publicationRequestPut.setTenants(Set.of("tenant1"));
    publicationRequestPut.setUrl("/organizations-storage/organizations/1844767a-8367-4926-9999-514c35840399");
    var publicationRequestPost = createPublicationRequestForSetting(sharingSettingRequest, HttpMethod.POST.toString());
    publicationRequestPost.setMethod("POST");
    publicationRequestPost.setTenants(Set.of("tenant2"));

    var publicationResponsePost = new PublicationResponse().id(createSettingsPcId);
    var publicationResponsePut = new PublicationResponse().id(updateSettingsPcId);

    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    when(publicationService.publishRequest(consortiumId, publicationRequestPost)).thenReturn(publicationResponsePost);
    when(publicationService.publishRequest(consortiumId, publicationRequestPut)).thenReturn(publicationResponsePut);
    when(tenantService.getAll(consortiumId)).thenReturn(tenantCollection);
    when(sharingSettingRepository.findTenantsBySettingId(sharingSettingRequest.getSettingId())).thenReturn(tenantAssociationsWithSetting);
    when(sharingSettingRepository.save(any())).thenReturn(new SharingSettingEntity());
    when(folioExecutionContext.getTenantId()).thenReturn("mobius");
    doReturn(folioExecutionContext).when(contextHelper).getSystemUserFolioExecutionContext(anyString());
    when(objectMapper.convertValue(payload, JsonNode.class)).thenReturn(createJsonNode());

    var expectedResponse = createSharingSettingResponse(createSettingsPcId, updateSettingsPcId);
    var actualResponse = sharingSettingService.start(consortiumId, sharingSettingRequest);

    assertThat(actualResponse.getCreateSettingsPCId()).isEqualTo(expectedResponse.getCreateSettingsPCId());
    assertThat(actualResponse.getUpdateSettingsPCId()).isEqualTo(expectedResponse.getUpdateSettingsPCId());

    verify(publicationService, times(2)).publishRequest(any(), any());
  }

  @Test
  void shouldDeleteSharingSetting() {
    UUID consortiumId = UUID.randomUUID();
    UUID pcId = UUID.randomUUID();
    UUID settingId = UUID.fromString("1844767a-8367-4926-9999-514c35840399");
    Tenant tenant1 = createTenant("tenant1", "tenant1");
    Tenant tenant2 = createTenant("tenant2", "tenant2");
    Set<String> tenantAssociationsWithSetting = Set.of("tenant1");
    TenantCollection tenantCollection = createTenantCollection(List.of(tenant1, tenant2));
    var sharingSettingRequest = getMockDataObject(SHARING_SETTING_REQUEST_SAMPLE_WITHOUT_PAYLOAD, SharingSettingRequest.class);

    // "tenant1" exists in tenant setting association so that tenant1 is in DELETE request publication,
    var publicationRequestDelete = createPublicationRequestForSetting(sharingSettingRequest, HttpMethod.DELETE.toString());
    publicationRequestDelete.setTenants(Set.of("tenant1"));
    publicationRequestDelete.setUrl("/organizations-storage/organizations/1844767a-8367-4926-9999-514c35840399");

    var publicationResponse = new PublicationResponse().id(pcId);

    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    when(sharingSettingRepository.existsBySettingId(settingId)).thenReturn(true);
    when(publicationService.publishRequest(consortiumId, publicationRequestDelete)).thenReturn(publicationResponse);
    when(tenantService.getAll(consortiumId)).thenReturn(tenantCollection);
    when(sharingSettingRepository.findTenantsBySettingId(sharingSettingRequest.getSettingId())).thenReturn(tenantAssociationsWithSetting);
    when(folioExecutionContext.getTenantId()).thenReturn("mobius");
    doReturn(folioExecutionContext).when(contextHelper).getSystemUserFolioExecutionContext(anyString());

    var expectedResponse = createSharingSettingResponseForDelete(pcId);
    var actualResponse = sharingSettingService.delete(consortiumId, settingId, sharingSettingRequest);

    assertThat(actualResponse.getPcId()).isEqualTo(expectedResponse.getPcId());

    verify(publicationService, times(1)).publishRequest(any(), any());
  }

  // Negative cases
  @Test
  void shouldThrowErrorForNotEqualSettingIdWithPayloadId() throws JsonProcessingException {
    UUID consortiumId = UUID.randomUUID();
    var sharingSettingRequest = getMockDataObject(SHARING_SETTING_REQUEST_SAMPLE, SharingSettingRequest.class);
    Map<String, String> payload = new LinkedHashMap<>();
    payload.put("id", "9999999-8367-4926-9999-514c35840399");
    payload.put("name", "ORG-NAME");
    payload.put("source", "local");
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(payload);
    JsonNode node = mapper.readTree(json);

    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    when(objectMapper.convertValue(any(), eq(JsonNode.class))).thenReturn(node);

    assertThrows(java.lang.IllegalArgumentException.class, () -> sharingSettingService.start(consortiumId, sharingSettingRequest));
    verify(publicationService, times(0)).publishRequest(any(), any());
  }

  @Test
  void shouldThrowErrorForNotEqualSettingIdPathId() {
    UUID consortiumId = UUID.randomUUID();
    UUID settingId = UUID.fromString("999999-8367-4926-9999-514c35840399");

    var sharingSettingRequest = getMockDataObject(SHARING_SETTING_REQUEST_SAMPLE_WITHOUT_PAYLOAD, SharingSettingRequest.class);

    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);

    assertThrows(java.lang.IllegalArgumentException.class, () -> sharingSettingService.delete(consortiumId, settingId, sharingSettingRequest));
    verify(publicationService, times(0)).publishRequest(any(), any());
  }

  @Test
  void shouldThrowErrorForNotFound() {
    UUID consortiumId = UUID.randomUUID();
    UUID settingId = UUID.fromString("1844767a-8367-4926-9999-514c35840399");

    var sharingSettingRequest = getMockDataObject(SHARING_SETTING_REQUEST_SAMPLE_WITHOUT_PAYLOAD, SharingSettingRequest.class);

    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    when(sharingSettingRepository.existsBySettingId(settingId)).thenReturn(false);

    assertThrows(org.folio.consortia.exception.ResourceNotFoundException.class, () -> sharingSettingService.delete(consortiumId, settingId, sharingSettingRequest));
    verify(publicationService, times(0)).publishRequest(any(), any());
  }

  public JsonNode createJsonNode() throws JsonProcessingException {
    Map<String, String> payload = new HashMap<>();
    payload.put("id", "1844767a-8367-4926-9999-514c35840399");
    payload.put("name", "ORG-NAME");
    payload.put("source", "local");
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(payload);
    return mapper.readTree(json);
  }
}
