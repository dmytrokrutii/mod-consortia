package org.folio.consortia.controller;

import org.folio.consortia.domain.entity.ConsortiumEntity;
import org.folio.consortia.domain.entity.TenantEntity;
import org.folio.consortia.domain.repository.ConsortiumRepository;
import org.folio.consortia.domain.repository.TenantRepository;
import org.folio.consortia.support.BaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EntityScan(basePackageClasses = TenantEntity.class)
class TenantControllerTest extends BaseTest {
  @MockBean
  ConsortiumRepository consortiumRepository;
  @MockBean
  TenantRepository tenantRepository;

  @Test
  void getTenants() throws Exception {
    TenantEntity tenantEntity1 = new TenantEntity();
    tenantEntity1.setId("ABC1");
    tenantEntity1.setName("TestName1");

    TenantEntity tenantEntity2 = new TenantEntity();
    tenantEntity1.setId("ABC1");
    tenantEntity1.setName("TestName1");
    List<TenantEntity> tenantEntityList = new ArrayList<>();
    tenantEntityList.add(tenantEntity1);
    tenantEntityList.add(tenantEntity2);
    ConsortiumEntity consortiumEntity = new ConsortiumEntity();
    consortiumEntity.setId(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"));
    consortiumEntity.setName("TestConsortium");

    when(tenantRepository.findByConsortiumId(any(), any(PageRequest.of(0, 2).getClass())))
      .thenReturn(new PageImpl<>(tenantEntityList, PageRequest.of(0, 2), tenantEntityList.size()));
    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(consortiumEntity));
    var headers = defaultHeaders();

    this.mockMvc.perform(get("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants?limit=2&offset=1").headers(headers))
      .andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE));
  }

  @Test
  void getBadRequest() throws Exception {
    var headers = defaultHeaders();
    ConsortiumEntity consortiumEntity = new ConsortiumEntity();
    consortiumEntity.setId(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"));
    consortiumEntity.setName("TestConsortium");

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(consortiumEntity));
    this.mockMvc.perform(get("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants?limit=0&offset=0").headers(headers))
      .andExpectAll(
        status().is4xxClientError(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE),
        jsonPath("$.errors[0].message", is("Page size must not be less than one")));
  }

  @Test
  void get4xxError() throws Exception {
    var headers = defaultHeaders();
    this.mockMvc.perform(get("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants?limit=0&offset=0").headers(headers))
      .andExpectAll(
        status().is4xxClientError(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE),
        jsonPath("$.errors[0].message", is("Object with id [07698e46-c3e3-11ed-afa1-0242ac120002] was not found")));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"id\":\"diku\",\"name\":\"diku_tenant_name\"}"
  })
  void shouldGet4xxErrorWhileSaving(String contentString) throws Exception {
    var headers = defaultHeaders();
    this.mockMvc.perform(
        post("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants")
          .headers(headers)
          .content(contentString))
        .andExpect(matchAll(status().is4xxClientError()));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"id\":\"diku\",\"name\":\"diku_tenant_name\"}"
  })
  void shouldGet4xxErrorWhileSavingDuplicateName(String contentString) throws Exception {
    var headers = defaultHeaders();
    ConsortiumEntity consortiumEntity = new ConsortiumEntity();
    consortiumEntity.setId(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"));
    consortiumEntity.setName("TestConsortium");
    TenantEntity tenant = new TenantEntity();

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(consortiumEntity));
    when(tenantRepository.findById(any(String.class))).thenReturn(Optional.of(tenant));
    when(tenantRepository.save(any(TenantEntity.class))).thenThrow(DataIntegrityViolationException.class);

    this.mockMvc.perform(
        post("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants")
          .headers(headers)
          .content(contentString))
      .andExpect(matchAll(status().is4xxClientError()));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"id\":\"diku1234\",\"name\":\"diku_tenant_name1234\"}"
  })
  void shouldSaveTenant(String contentString) throws Exception {
    var headers = defaultHeaders();
    ConsortiumEntity consortiumEntity = new ConsortiumEntity();
    consortiumEntity.setId(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"));
    consortiumEntity.setName("TestConsortium");

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(consortiumEntity));
    when(tenantRepository.save(any(TenantEntity.class))).thenReturn(TenantEntity.class.newInstance());

    this.mockMvc.perform(
        post("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/tenants")
          .headers(headers)
          .content(contentString))
      .andExpect(matchAll(status().isOk()));
  }
}
