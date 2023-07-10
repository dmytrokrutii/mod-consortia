package org.folio.consortia.controller;

import static org.folio.consortia.utils.EntityUtils.createPublicationTenantRequestEntity;
import static org.folio.consortia.utils.InputOutputTestUtils.getMockDataAsString;
import static org.folio.consortia.utils.InputOutputTestUtils.getMockDataObject;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.consortia.domain.dto.PublicationStatus;
import org.folio.consortia.domain.entity.PublicationStatusEntity;
import org.folio.consortia.domain.entity.PublicationTenantRequestEntity;
import org.folio.consortia.repository.PublicationStatusRepository;
import org.folio.consortia.repository.PublicationTenantRequestRepository;
import org.folio.consortia.service.ConsortiumService;
import org.folio.consortia.service.HttpRequestService;
import org.folio.consortia.service.TenantService;
import org.folio.consortia.service.UserTenantService;
import org.folio.consortia.support.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

public class PublicationControllerTest extends BaseIT {
  public static final String PUBLICATIONS_URL = "/consortia/%s/publications";
  public static final String GET_PUBLICATION_BY_ID_URL = "/consortia/%s/publications/%s";
  @MockBean
  TenantService tenantService;
  @MockBean
  UserTenantService userTenantService;
  @MockBean
  HttpRequestService httpRequestService;
  @MockBean
  PublicationStatusRepository publicationStatusRepository;
  @MockBean
  PublicationTenantRequestRepository publicationTenantRequestRepository;
  @MockBean
  ConsortiumService consortiumService;

  @Test
  void publicationSuccessful() throws Exception {
    var headers = defaultHeaders();
    var consortiumId = UUID.randomUUID();
    var publicationString = getMockDataAsString("mockdata/publications/publication_request.json");
    var publicationStatusEntity = getMockDataObject("mockdata/publications/publication_status_entity.json", PublicationStatusEntity.class);
    publicationStatusEntity.setId(UUID.randomUUID());

    doNothing().when(tenantService).checkTenantsAndConsortiumExistsOrThrow(any(UUID.class), any());
    when(userTenantService.checkUserIfHasPrimaryAffiliationByUserId(any(UUID.class), any())).thenReturn(true);
    when(publicationStatusRepository.save(any())).thenReturn(publicationStatusEntity);

    this.mockMvc.perform(post(String.format(PUBLICATIONS_URL, consortiumId)).headers(headers)
      .content(publicationString))
      .andExpectAll(status().is2xxSuccessful());
  }

  @Test
  void publicationWithTenantException() throws Exception {
    var headers = defaultHeaders();
    var consortiumId = UUID.randomUUID();
    var publicationString = getMockDataAsString("mockdata/publications/publication_request.json");
    var publicationStatusEntity = getMockDataObject("mockdata/publications/publication_status_entity.json", PublicationStatusEntity.class);
    publicationStatusEntity.setId(UUID.randomUUID());

    var respEntity = new ResponseEntity<>(publicationString, HttpStatusCode.valueOf(400));

    doNothing().when(tenantService).checkTenantsAndConsortiumExistsOrThrow(any(UUID.class), any());
    when(userTenantService.checkUserIfHasPrimaryAffiliationByUserId(any(UUID.class), any())).thenReturn(true);
    when(httpRequestService.performRequest(anyString(), eq(HttpMethod.POST), any())).thenReturn(respEntity);
    when(publicationStatusRepository.save(any())).thenReturn(publicationStatusEntity);

    this.mockMvc.perform(post(String.format(PUBLICATIONS_URL, consortiumId)).headers(headers)
        .content(publicationString))
      .andExpectAll(status().is2xxSuccessful());
  }
  @Test
  void publicationPreValidationError() throws Exception {
    var headers = defaultHeaders();
    var consortiumId = UUID.randomUUID();
    var publicationString = getMockDataAsString("mockdata/publications/publication_request.json");

    doNothing().when(tenantService).checkTenantsAndConsortiumExistsOrThrow(any(UUID.class), any());
    when(userTenantService.checkUserIfHasPrimaryAffiliationByUserId(any(UUID.class), any())).thenReturn(false);

    this.mockMvc.perform(post(String.format(PUBLICATIONS_URL, consortiumId)).headers(headers)
        .content(publicationString))
      .andExpectAll(status().is4xxClientError());
  }

  @Test
  void getPublicationSuccessful() throws Exception {
    var headers = defaultHeaders();
    var consortiumId = UUID.randomUUID();
    var publicationId = UUID.randomUUID();

    var publicationStatusEntity = getMockDataObject("mockdata/publications/publication_status_entity.json", PublicationStatusEntity.class);
    publicationStatusEntity.setStatus(PublicationStatus.ERROR);
    publicationStatusEntity.setCreatedDate(LocalDateTime.now());

    doNothing().when(consortiumService).checkConsortiumExistsOrThrow(any(UUID.class));
    when(publicationStatusRepository.findById(publicationId)).thenReturn(Optional.of(publicationStatusEntity));

    var tenantRequest1 = createPublicationTenantRequestEntity(publicationStatusEntity, TENANT, PublicationStatus.COMPLETE, 201);
    var tenantRequest2 = createPublicationTenantRequestEntity(publicationStatusEntity, TENANT, PublicationStatus.ERROR, 400);
    List<PublicationTenantRequestEntity> ptrEntityMockResponse = List.of(tenantRequest1, tenantRequest2);

    Page<PublicationTenantRequestEntity> ptrEntities  = new PageImpl<>(ptrEntityMockResponse);
    when(publicationTenantRequestRepository.findByPcStateId(eq(publicationId), any())).thenReturn(ptrEntities);

    this.mockMvc.perform(get(String.format(GET_PUBLICATION_BY_ID_URL, consortiumId, publicationId)).headers(headers))
      .andExpectAll(
        status().is2xxSuccessful(),
        jsonPath("$.status", is(PublicationStatus.ERROR.getValue())),
        jsonPath("$.errors", is(not(empty())))
      );
  }

}
