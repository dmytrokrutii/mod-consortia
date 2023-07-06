package org.folio.consortia.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.folio.consortia.support.BaseUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;


class HttpRequestServiceImplTest extends BaseUnitTest {

  @InjectMocks
  HttpRequestServiceImpl httpRequestService;
  @Mock
  RestTemplate restTemplate;
  @Test
  void performRequestSuccess() {
    var payload = RandomStringUtils.random(10);
    ResponseEntity<String> restTemplateResponse = new ResponseEntity<>(payload, HttpStatusCode.valueOf(201));
    when(folioExecutionContext.getTenantId()).thenReturn(CENTRAL_TENANT_NAME);
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(defaultHeaders());
    when(folioExecutionContext.getAllHeaders()).thenReturn(defaultHeaders());
    when(restTemplate.exchange(
      anyString(),
      eq(HttpMethod.POST),
      Mockito.any(HttpEntity.class),
      Mockito.eq(String.class))
    ).thenReturn(restTemplateResponse);

    var response = httpRequestService.performRequest(payload, HttpMethod.POST, new Object());
    Assertions.assertEquals(payload, response.getBody());
  }
}
