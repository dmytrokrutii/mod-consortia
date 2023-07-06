package org.folio.consortia.service.impl;

import static org.folio.consortia.utils.InputOutputTestUtils.getMockDataObject;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.concurrent.CompletionException;

import org.folio.consortia.domain.dto.PublicationRequest;
import org.folio.consortia.domain.dto.PublicationStatus;
import org.folio.consortia.domain.entity.PublicationStatusEntity;
import org.folio.consortia.domain.entity.PublicationTenantRequestEntity;
import org.folio.consortia.repository.PublicationStatusRepository;
import org.folio.consortia.repository.PublicationTenantRequestRepository;
import org.folio.consortia.service.HttpRequestService;
import org.folio.consortia.support.BaseUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class PublicationServiceImplTest extends BaseUnitTest {
  private static final String PUBLICATION_REQUEST_SAMPLE = "mockdata/publications/publication_request.json";
  private static final String PUBLICATION_STATUS_ENTITY_SAMPLE = "mockdata/publications/publication_status_entity.json";
  @InjectMocks
  private PublicationServiceImpl publicationService;
  @Mock
  PublicationTenantRequestRepository publicationTenantRequestRepository;
  @Mock
  PublicationStatusRepository publicationStatusRepository;
  @Mock
  HttpRequestService httpRequestService;
  @Mock
  ObjectMapper objectMapper;
  @Captor
  ArgumentCaptor<PublicationTenantRequestEntity> ptreCaptor;

  @Test
  void createTenantRequestEntitiesSuccess() throws JsonProcessingException {
    PublicationRequest pr = getMockDataObject(PUBLICATION_REQUEST_SAMPLE, PublicationRequest.class);
    var publicationStatusEntity = getMockDataObject(PUBLICATION_STATUS_ENTITY_SAMPLE, PublicationStatusEntity.class);
    publicationStatusEntity.setCreatedDate(LocalDateTime.now());

    when(objectMapper.writeValueAsString(anyString())).thenReturn(RandomStringUtils.random(10));
    when(publicationTenantRequestRepository.save(any(PublicationTenantRequestEntity.class))).thenReturn(new PublicationTenantRequestEntity());

    publicationService.processTenantRequests(pr, publicationStatusEntity);

    verify(publicationTenantRequestRepository, atLeast(pr.getTenants().size())).save(any());
  }

  @Test
  void executeAsyncHttpRequestSuccess() throws JsonProcessingException {
    var pr = getMockDataObject(PUBLICATION_REQUEST_SAMPLE, PublicationRequest.class);
    var payload = objectMapper.writeValueAsString(pr.getPayload());
    var publicationStatusEntity = getMockDataObject(PUBLICATION_STATUS_ENTITY_SAMPLE, PublicationStatusEntity.class);
    publicationStatusEntity.setCreatedDate(LocalDateTime.now());

    when(objectMapper.writeValueAsString(anyString())).thenReturn(RandomStringUtils.random(10));
    when(publicationTenantRequestRepository.save(any(PublicationTenantRequestEntity.class))).thenReturn(new PublicationTenantRequestEntity());

    ResponseEntity<String> restTemplateResponse = new ResponseEntity<>(payload, HttpStatusCode.valueOf(201));
    when(httpRequestService.performRequest(anyString(), eq(HttpMethod.POST), any())).thenReturn(restTemplateResponse);
    var response = publicationService.executeAsyncHttpRequest(pr, CENTRAL_TENANT_NAME, folioExecutionContext).join();
    Assertions.assertEquals(payload, response.getBody());
  }
  @Test
  void executeAsyncHttpWithErrorResponse() throws JsonProcessingException {
    var pr = getMockDataObject(PUBLICATION_REQUEST_SAMPLE, PublicationRequest.class);
    var payload = objectMapper.writeValueAsString(pr.getPayload());
    var publicationStatusEntity = getMockDataObject(PUBLICATION_STATUS_ENTITY_SAMPLE, PublicationStatusEntity.class);
    publicationStatusEntity.setCreatedDate(LocalDateTime.now());

    when(objectMapper.writeValueAsString(anyString())).thenReturn(RandomStringUtils.random(10));
    when(publicationTenantRequestRepository.save(any(PublicationTenantRequestEntity.class))).thenReturn(new PublicationTenantRequestEntity());

    ResponseEntity<String> restTemplateResponse = new ResponseEntity<>(payload, HttpStatusCode.valueOf(301));
    when(httpRequestService.performRequest(anyString(), eq(HttpMethod.POST), any())).thenReturn(restTemplateResponse);

    var future = publicationService.executeAsyncHttpRequest(pr, CENTRAL_TENANT_NAME, folioExecutionContext);

    assertThrowsCause(HttpClientErrorException.class, future::join);
  }

  @Test
  void executeAsyncHttpFailure() throws JsonProcessingException {
    var pr = getMockDataObject(PUBLICATION_REQUEST_SAMPLE, PublicationRequest.class);
    var publicationStatusEntity = getMockDataObject(PUBLICATION_STATUS_ENTITY_SAMPLE, PublicationStatusEntity.class);
    publicationStatusEntity.setCreatedDate(LocalDateTime.now());

    when(objectMapper.writeValueAsString(anyString())).thenReturn(RandomStringUtils.random(10));
    when(publicationTenantRequestRepository.save(any(PublicationTenantRequestEntity.class))).thenReturn(new PublicationTenantRequestEntity());

    when(httpRequestService.performRequest(anyString(), eq(HttpMethod.POST), any()))
      .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase()));

    var future = publicationService.executeAsyncHttpRequest(pr, CENTRAL_TENANT_NAME, folioExecutionContext);

    assertThrowsCause(HttpClientErrorException.class, future::join);
  }


  @Test
  void updatePublicationTenantRequestOnSuccess() {
    PublicationTenantRequestEntity ptrEntity = new PublicationTenantRequestEntity();
    ptrEntity.setStatus(PublicationStatus.IN_PROGRESS);
    when(publicationTenantRequestRepository.save(any(PublicationTenantRequestEntity.class))).thenReturn(new PublicationTenantRequestEntity());
    when(publicationStatusRepository.save(any(PublicationStatusEntity.class))).thenReturn(new PublicationStatusEntity());

    var payload = RandomStringUtils.random(10);
    ResponseEntity<String> restTemplateResponse = new ResponseEntity<>(payload, HttpStatusCode.valueOf(201));

    publicationService.updatePublicationTenantRequest(restTemplateResponse, null, ptrEntity, folioExecutionContext);
    verify(publicationTenantRequestRepository).save(ptreCaptor.capture());

    var capturedPtre = ptreCaptor.getValue();
    Assertions.assertEquals(PublicationStatus.COMPLETE, capturedPtre.getStatus());
    Assertions.assertEquals(payload, capturedPtre.getResponse());
    Assertions.assertEquals(HttpStatus.CREATED.value(), capturedPtre.getResponseStatusCode());
  }

  @Test
  void updatePublicationTenantRequestOnFailure() {
    PublicationTenantRequestEntity ptrEntity = new PublicationTenantRequestEntity();
    ptrEntity.setStatus(PublicationStatus.IN_PROGRESS);
    when(publicationTenantRequestRepository.save(any(PublicationTenantRequestEntity.class))).thenReturn(new PublicationTenantRequestEntity());

    Throwable t = new CompletionException(new HttpClientErrorException(HttpStatusCode.valueOf(400), HttpStatus.BAD_REQUEST.getReasonPhrase()));
    publicationService.updatePublicationTenantRequest(null, t, ptrEntity, folioExecutionContext);
    verify(publicationTenantRequestRepository).save(ptreCaptor.capture());

    var capturedPtre = ptreCaptor.getValue();
    Assertions.assertEquals(PublicationStatus.ERROR, capturedPtre.getStatus());
    Assertions.assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), capturedPtre.getResponse());
    Assertions.assertEquals(HttpStatus.BAD_REQUEST.value(), capturedPtre.getResponseStatusCode());
  }

}
