package org.folio.consortia.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.folio.consortia.client.ConsortiaConfigurationClient;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.repository.ConsortiumRepository;
import org.folio.consortia.repository.TenantRepository;
import org.folio.consortia.repository.UserTenantRepository;
import org.folio.consortia.service.impl.ConsortiaConfigurationServiceImpl;
import org.folio.consortia.support.BaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.folio.consortia.utils.EntityUtils.createConsortiaConfiguration;
import static org.folio.consortia.utils.EntityUtils.createTenantEntity;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EntityScan(basePackageClasses = TenantEntity.class)
class TenantControllerTest extends BaseTest {
  private static final String TENANT_REQUEST_BODY = "{\"id\":\"diku1234\",\"code\":\"TST\",\"name\":\"diku_tenant_name1234\", \"isCentral\":false}";
  private static final String CONSORTIUM_ID = "7698e46-c3e3-11ed-afa1-0242ac120002";
  private static final String CENTRAL_TENANT_ID = "diku";
  @MockBean
  ConsortiumRepository consortiumRepository;
  @MockBean
  TenantRepository tenantRepository;
  @MockBean
  UserTenantRepository userTenantRepository;
  @MockBean
  ConsortiaConfigurationServiceImpl configurationService;
  @MockBean
  ConsortiaConfigurationClient configurationClient;

  /* Success cases */
  @Test
  void getTenants() throws Exception {
    UUID consortiumId = UUID.fromString(CONSORTIUM_ID);
    TenantEntity tenantEntity1 = createTenantEntity();
    TenantEntity tenantEntity2 = createTenantEntity();
    List<TenantEntity> tenantEntityList = new ArrayList<>();
    tenantEntityList.add(tenantEntity1);
    tenantEntityList.add(tenantEntity2);

    when(tenantRepository.findByConsortiumId(any(), any(PageRequest.of(0, 2)
      .getClass()))).thenReturn(new PageImpl<>(tenantEntityList, PageRequest.of(0, 2), tenantEntityList.size()));
    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    var headers = defaultHeaders();

    this.mockMvc.perform(get("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants?limit=2&offset=1").headers(headers))
      .andExpectAll(status().isOk(), content().contentType(MediaType.APPLICATION_JSON_VALUE));
  }

  @ParameterizedTest
  @ValueSource(strings = {TENANT_REQUEST_BODY})
  void shouldSaveTenant(String contentString) throws Exception {
    var headers = defaultHeaders();
    TenantEntity centralTenant = createTenantEntity(CENTRAL_TENANT_ID, CENTRAL_TENANT_ID, "AAA", true);

    when(consortiumRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.existsById(any())).thenReturn(false);
    when(tenantRepository.save(any(TenantEntity.class))).thenReturn(new TenantEntity());
    when(tenantRepository.findCentralTenant()).thenReturn(Optional.of(centralTenant));
    doNothing().when(configurationClient).saveConfiguration(createConsortiaConfiguration(CENTRAL_TENANT_ID));

    this.mockMvc.perform(
        post("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants")
          .headers(headers).content(contentString))
      .andExpect(status().isCreated());
  }

  @ParameterizedTest
  @ValueSource(strings = {TENANT_REQUEST_BODY})
  void shouldUpdateTenant(String contentString) throws Exception {
    TenantEntity tenant = createTenantEntity();

    var headers = defaultHeaders();
    when(tenantRepository.existsById(any())).thenReturn(true);
    when(consortiumRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.save(any())).thenReturn(tenant);

    this.mockMvc.perform(
        put("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants/diku1234")
          .headers(headers).content(contentString))
      .andExpectAll(status().isOk());
  }

  /* Error cases */
  @Test
  void getBadRequest() throws Exception {
    var headers = defaultHeaders();
    UUID consortiumId = UUID.fromString(CONSORTIUM_ID);

    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);

    this.mockMvc.perform(get("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants?limit=0&offset=0")
        .headers(headers)).
      andExpectAll(
        status().is4xxClientError(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE),
        jsonPath("$.errors[0].message", is("Page size must not be less than one")));
  }

  @Test
  void get4xxError() throws Exception {
    var headers = defaultHeaders();

    this.mockMvc.perform(get("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants?limit=0&offset=0")
        .headers(headers))
      .andExpectAll(
        status().is4xxClientError(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE),
        jsonPath("$.errors[0].message", is("Object with consortiumId [07698e46-c3e3-11ed-afa1-0242ac120002] was not found")));
  }

  @ParameterizedTest
  @ValueSource(strings = {TENANT_REQUEST_BODY})
  void shouldGet4xxErrorWhileSaving(String contentString) throws Exception {
    var headers = defaultHeaders();
    TenantEntity centralTenant = createTenantEntity(CENTRAL_TENANT_ID, CENTRAL_TENANT_ID, "TTA", true);

    when(tenantRepository.findCentralTenant()).thenReturn(Optional.of(centralTenant));
    doNothing().when(configurationClient).saveConfiguration(createConsortiaConfiguration(CENTRAL_TENANT_ID));

    this.mockMvc.perform(post("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants")
        .headers(headers).content(contentString))
      .andExpectAll(
        status().is4xxClientError(),
        jsonPath("$.errors[0].message", is("Object with consortiumId [07698e46-c3e3-11ed-afa1-0242ac120002] was not found")),
        jsonPath("$.errors[0].code", is("NOT_FOUND_ERROR")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{\"id\": \"123123123123123123\",\"code\":\"TST\", \"name\": \"\"}"})
    // isCentral is not given
  void shouldThrowMethodArgumentNotValidationException(String contentString) throws Exception {
    var headers = defaultHeaders();

    mockMvc.perform(post("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants")
        .headers(headers)
        .contentType(MediaType.APPLICATION_JSON)
        .content(contentString))
      .andExpect(status().isUnprocessableEntity())
      .andExpect(jsonPath("$.errors.size()", is(1)))
      .andExpect(jsonPath("$.errors[0].code", is("tenantValidationError")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{\"id\": \"123123123123123123\",\"code\":\"TST\", \"name\": \"\", \"isCentral\":false}"})
  void shouldThrownConstraintViolationException(String contentString) throws Exception {
    var headers = defaultHeaders();
    TenantEntity centralTenant = createTenantEntity(CENTRAL_TENANT_ID, CENTRAL_TENANT_ID, "TTA", true);

    // Given a request with invalid input
    UUID consortiumId = UUID.fromString(CONSORTIUM_ID);
    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    when(tenantRepository.existsById(any(String.class))).thenReturn(false);
    when(tenantRepository.findCentralTenant()).thenReturn(Optional.of(centralTenant));
    doNothing().when(configurationClient).saveConfiguration(createConsortiaConfiguration(CENTRAL_TENANT_ID));

    Set<ConstraintViolation<?>> constraintViolations = new HashSet<>();
    constraintViolations.add(mock(ConstraintViolation.class));
    constraintViolations.add(mock(ConstraintViolation.class));

    when(tenantRepository.save(any(TenantEntity.class)))
      .thenThrow(new ConstraintViolationException("Invalid input", constraintViolations));

    // When performing a request to the endpoint
    mockMvc.perform(post("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants")
        .headers(headers)
        .contentType(MediaType.APPLICATION_JSON)
        .content(contentString))
      .andExpect(status().isUnprocessableEntity())
      .andExpect(jsonPath("$.errors.size()", is(2)))
      .andExpect(jsonPath("$.errors[0].code", is("ValidationError")))
      .andExpect(jsonPath("$.errors[0].type", is("-1")))
      .andExpect(jsonPath("$.errors[1].code", is("ValidationError")))
      .andExpect(jsonPath("$.errors[1].type", is("-1")));
  }


  @ParameterizedTest
  @ValueSource(strings = {TENANT_REQUEST_BODY})
  void shouldGet4xxErrorWhileSavingDuplicateName(String contentString) throws Exception {
    var headers = defaultHeaders();
    UUID consortiumId = UUID.fromString(CONSORTIUM_ID);
    TenantEntity centralTenant = createTenantEntity(CENTRAL_TENANT_ID, CENTRAL_TENANT_ID, "TTA", true);

    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    when(tenantRepository.existsById(any(String.class))).thenReturn(true);
    when(tenantRepository.findCentralTenant()).thenReturn(Optional.of(centralTenant));
    doNothing().when(configurationClient).saveConfiguration(createConsortiaConfiguration(CENTRAL_TENANT_ID));

    this.mockMvc.perform(
        post("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants")
          .headers(headers).content(contentString))
      .andExpectAll(
        status().is4xxClientError(),
        jsonPath("$.errors[0].message", is("Object with id [diku1234] is already presented in the system")),
        jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));
  }

  @ParameterizedTest
  @ValueSource(strings = {TENANT_REQUEST_BODY})
  void shouldThrowValidationErrorWhileUpdateTenant(String contentString) throws Exception {
    var headers = defaultHeaders();

    when(tenantRepository.existsById(any())).thenReturn(true);
    when(consortiumRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.save(any(TenantEntity.class))).thenReturn(TenantEntity.class.newInstance());

    this.mockMvc.perform(
        put("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants/TestID")
          .headers(headers).content(contentString))
      .andExpectAll(
        status().is4xxClientError(),
        jsonPath("$.errors[0].message", is("Request body tenantId and path param tenantId should be identical")),
        jsonPath("$.errors[0].code", is("VALIDATION_ERROR")));
  }

  @ParameterizedTest
  @ValueSource(strings = {TENANT_REQUEST_BODY})
  void shouldThrowNotFoundErrorWhileUpdateTenant(String contentString) throws Exception {
    TenantEntity tenant = createTenantEntity();
    var headers = defaultHeaders();

    when(tenantRepository.existsById(any())).thenReturn(true);
    when(consortiumRepository.existsById(any())).thenReturn(false);
    when(tenantRepository.save(tenant)).thenReturn(tenant);

    this.mockMvc.perform(
        put("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants/diku1234")
          .headers(headers).content(contentString))
      .andExpectAll(
        status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("NOT_FOUND_ERROR")));
  }

  @ParameterizedTest
  @ValueSource(strings = {TENANT_REQUEST_BODY})
  void shouldThrownHasActiveAffiliationExceptionWhileDeletingTenant(String contentString) throws Exception {
    var headers = defaultHeaders();
    when(tenantRepository.existsById(any())).thenReturn(true);
    when(consortiumRepository.existsById(any())).thenReturn(true);
    when(userTenantRepository.existsByTenantId("diku1234")).thenReturn(true);

    this.mockMvc.perform(
        delete("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants/diku1234")
          .headers(headers).content(contentString))
      .andExpectAll(
        status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("VALIDATION_ERROR")));
  }
}
