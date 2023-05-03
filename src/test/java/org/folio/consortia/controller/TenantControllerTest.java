package org.folio.consortia.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.repository.ConsortiumRepository;
import org.folio.consortia.repository.TenantRepository;
import org.folio.consortia.repository.UserTenantRepository;
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
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
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

  @MockBean
  ConsortiumRepository consortiumRepository;
  @MockBean
  TenantRepository tenantRepository;
  @MockBean
  UserTenantRepository userTenantRepository;

  private static final String TENANT_REQUEST_BODY = "{\"id\":\"diku1234\",\"code\":\"TST\",\"name\":\"diku_tenant_name1234\"}";
  private static final String CONSORTIUM_ID = "7698e46-c3e3-11ed-afa1-0242ac120002";

  @Test
  void getTenants() throws Exception {
    UUID consortiumId = UUID.fromString(CONSORTIUM_ID);
    TenantEntity tenantEntity1 = createTenantEntity("ABC", "TestName1", "TST");
    TenantEntity tenantEntity2 = createTenantEntity("ABC", "TestName1", "TST");
    List<TenantEntity> tenantEntityList = new ArrayList<>();
    tenantEntityList.add(tenantEntity1);
    tenantEntityList.add(tenantEntity2);

    when(tenantRepository.findByConsortiumId(any(), any(PageRequest.of(0, 2).getClass()))).thenReturn(new PageImpl<>(tenantEntityList, PageRequest.of(0, 2), tenantEntityList.size()));
    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    var headers = defaultHeaders();

    this.mockMvc.perform(get("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants?limit=2&offset=1").headers(headers)).andExpectAll(status().isOk(), content().contentType(MediaType.APPLICATION_JSON_VALUE));
  }

  @ParameterizedTest
  @ValueSource(strings = {TENANT_REQUEST_BODY})
  void shouldSaveTenant(String contentString) throws Exception {
    var headers = defaultHeaders();
    when(consortiumRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.existsById(any())).thenReturn(false);
    when(tenantRepository.save(any(TenantEntity.class))).thenReturn(new TenantEntity());

    this.mockMvc.perform(
        post("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants")
          .headers(headers).content(contentString))
      .andExpect(status().isCreated());
  }

  @ParameterizedTest
  @ValueSource(strings = {TENANT_REQUEST_BODY})
  void shouldUpdateTenant(String contentString) throws Exception {
    TenantEntity tenantEntity1 = createTenantEntity("diku1234", "TestName1", "TST");

    var headers = defaultHeaders();
    when(tenantRepository.existsById(any())).thenReturn(true);
    when(consortiumRepository.existsById(any())).thenReturn(true);
    when(tenantRepository.save(tenantEntity1)).thenReturn(tenantEntity1);

    this.mockMvc.perform(put("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants/diku1234").headers(headers).content(contentString)).andExpectAll(status().isOk());
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
  @ValueSource(strings = {"{\"id\":\"diku\",\"code\":\"TST\",\"name\":\"diku_tenant_name\"}"})
  void shouldGet4xxErrorWhileSaving(String contentString) throws Exception {
    var headers = defaultHeaders();

    this.mockMvc.perform(post("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants")
        .headers(headers).content(contentString))
      .andExpectAll(
        status().is4xxClientError(),
        jsonPath("$.errors[0].message", is("Object with consortiumId [07698e46-c3e3-11ed-afa1-0242ac120002] was not found")),
        jsonPath("$.errors[0].code", is("NOT_FOUND_ERROR")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{\"id\": \"123123123123123123\",\"code\":\"TST\", \"name\": \"\"}"})
  void shouldThrownConstraintViolationException(String contentString) throws Exception {
    var headers = defaultHeaders();
    // Given a request with invalid input
    UUID consortiumId = UUID.fromString(CONSORTIUM_ID);
    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    when(tenantRepository.existsById(any(String.class))).thenReturn(false);

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
  @ValueSource(strings = {"{\"id\":\"diku\",\"code\":\"TST\",\"name\":\"diku_tenant_name\"}"})
  void shouldGet4xxErrorWhileSavingDuplicateName(String contentString) throws Exception {
    var headers = defaultHeaders();
    UUID consortiumId = UUID.fromString(CONSORTIUM_ID);

    when(consortiumRepository.existsById(consortiumId)).thenReturn(true);
    when(tenantRepository.existsById(any(String.class))).thenReturn(true);

    this.mockMvc.perform(
        post("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants")
          .headers(headers).content(contentString))
      .andExpectAll(
        status().is4xxClientError(),
        jsonPath("$.errors[0].message", is("Object with id [diku] is already presented in the system")),
        jsonPath("$.errors[0].code", is("DUPLICATE_ERROR")));
  }

  @ParameterizedTest
  @ValueSource(strings = {TENANT_REQUEST_BODY})
  void shouldThrowValidationErrorWhileUpdateTenant(String contentString) throws Exception {
    TenantEntity tenantEntity1 = new TenantEntity();
    tenantEntity1.setId("TestID");
    tenantEntity1.setName("TestName1");

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
    TenantEntity tenantEntity1 = createTenantEntity("ABC1", "TestName1", "TST");
    var headers = defaultHeaders();
    when(tenantRepository.existsById(any())).thenReturn(true);
    when(consortiumRepository.existsById(any())).thenReturn(false);
    when(tenantRepository.save(tenantEntity1)).thenReturn(tenantEntity1);

    this.mockMvc.perform(
        put("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants/diku1234")
          .headers(headers).content(contentString))
      .andExpectAll(status().is4xxClientError(), jsonPath("$.errors[0].code", is("NOT_FOUND_ERROR")));
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

  private TenantEntity createTenantEntity(String id, String name, String code) {
    TenantEntity tenantEntity = new TenantEntity();
    tenantEntity.setId(id);
    tenantEntity.setCode(code);
    tenantEntity.setName(name);
    return tenantEntity;
  }
}
