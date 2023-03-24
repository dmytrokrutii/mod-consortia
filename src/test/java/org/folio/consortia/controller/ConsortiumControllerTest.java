package org.folio.consortia.controller;

import org.folio.consortia.domain.entity.ConsortiumEntity;
import org.folio.consortia.domain.repository.ConsortiumRepository;
import org.folio.consortia.exception.ResourceAlreadyExistException;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.folio.consortia.support.BaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConsortiumControllerTest extends BaseTest {
  private static final String CONSORTIUM_RESOURCE_EXIST_MSG_TEMPLATE = "System can not have more than one consortium record";

  @MockBean
  ConsortiumRepository consortiumRepository;

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"id\":\"111841e3-e6fb-4191-8fd8-5674a5107c33\",\"name\":\"consortium_name\"}"
  })
  void shouldGet4xxErrorWhileSaving(String contentString) throws Exception {
    var headers = defaultHeaders();
    when(consortiumRepository.count()).thenThrow(new ResourceAlreadyExistException(CONSORTIUM_RESOURCE_EXIST_MSG_TEMPLATE));
    this.mockMvc.perform(
        post("/consortia")
          .headers(headers)
          .content(contentString))
        .andExpect(matchAll(status().is4xxClientError(),
          jsonPath("$.errors[0].code", is("RESOURCE_ALREADY_EXIST")),
          jsonPath("$.errors[0].message", is("System can not have more than one consortium record"))));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"id\":\"111841e3-e6fb-4191-8fd8-5674a5107c33\",\"name\":\"consortium_name\"}"
  })
  void shouldSaveConsortium(String contentString) throws Exception {
    var headers = defaultHeaders();
    ConsortiumEntity consortiumEntity = createConsortiumEntity("111841e3-e6fb-4191-8fd8-5674a5107c33", "Test");
    when(consortiumRepository.save(any(ConsortiumEntity.class))).thenReturn(consortiumEntity);

    this.mockMvc.perform(
        post("/consortia")
          .headers(headers)
          .content(contentString))
      .andExpect(matchAll(status().isOk()));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"id\":\"111841e3-e6fb-4191-8fd8-5674a5107c33\"}"
  })
  void shouldGetConsortium(String contentString) throws Exception {
    var headers = defaultHeaders();
    ConsortiumEntity consortiumEntity = createConsortiumEntity("111841e3-e6fb-4191-8fd8-5674a5107c33", "Test");
    when(consortiumRepository.findById(any())).thenReturn(Optional.of(consortiumEntity));

    this.mockMvc.perform(
        get("/consortia/111841e3-e6fb-4191-8fd8-5674a5107c33")
          .headers(headers)
          .content(contentString))
      .andExpect(matchAll(status().isOk()));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"id\":\"111841e3-e6fb-4191-8fd8-5674a5107c33\"}"
  })
  void shouldThrowNotFoundWhileGetConsortium(String contentString) throws Exception {
    var headers = defaultHeaders();
    ConsortiumEntity consortiumEntity = createConsortiumEntity("111841e3-e6fb-4191-8fd8-5674a5107c33", "Test");
    when(consortiumRepository.findById(any())).thenThrow(ResourceNotFoundException.class);

    this.mockMvc.perform(
        get("/consortia/111841e3-e6fb-4191-8fd8-5674a5107c33")
          .headers(headers)
          .content(contentString))
      .andExpect(matchAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("NOT_FOUND_ERROR"))));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"id\":\"111841e3-e6fb-4191-8fd8-5674a5107c33\",\"name\":\"consortium_name\"}"
  })
  void shouldThrowNotFoundWhileUpdateConsortium(String contentString) throws Exception {
    var headers = defaultHeaders();
    ConsortiumEntity consortiumEntity = createConsortiumEntity("111841e3-e6fb-4191-8fd8-5674a5107c33", "Test");
    when(consortiumRepository.findById(any())).thenThrow(ResourceNotFoundException.class);

    this.mockMvc.perform(
        put("/consortia/111841e3-e6fb-4191-8fd8-5674a5107c33")
          .headers(headers)
          .content(contentString))
      .andExpect(matchAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("NOT_FOUND_ERROR"))));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"id\":\"111841e3-e6fb-4191-8fd8-5674a5107c33\",\"name\":\"consortium_name\"}"
  })
  void shouldThrowNotIdenticalWhileUpdateConsortium(String contentString) throws Exception {
    var headers = defaultHeaders();
    ConsortiumEntity consortiumEntity = createConsortiumEntity("111841e3-e6fb-4191-8fd8-5674a5107c13", "Test");
    when(consortiumRepository.findById(any())).thenReturn(Optional.of(consortiumEntity));

    this.mockMvc.perform(
        put("/consortia/111841e3-e6fb-4191-8fd8-5674a5107c34")
          .headers(headers)
          .content(contentString))
      .andExpect(matchAll(status().is4xxClientError(),
        jsonPath("$.errors[0].message", is("Request body consortiumId and path param consortiumId should be identical")),
        jsonPath("$.errors[0].code", is("VALIDATION_ERROR"))));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "{\"id\":\"111841e3-e6fb-4191-8fd8-5674a5107c33\",\"name\":\"consortium_name\"}"
  })
  void shouldUpdateConsortium(String contentString) throws Exception {
    var headers = defaultHeaders();
    ConsortiumEntity consortiumEntity = createConsortiumEntity("111841e3-e6fb-4191-8fd8-5674a5107c33", "Test");
    when(consortiumRepository.findById(any())).thenReturn(Optional.of(consortiumEntity));
    when(consortiumRepository.save(any())).thenReturn(consortiumEntity);

    this.mockMvc.perform(
        put("/consortia/111841e3-e6fb-4191-8fd8-5674a5107c33")
          .headers(headers)
          .content(contentString))
      .andExpect(matchAll(status().isOk()));
  }

  @Test
  void shouldGetConsortiumCollection() throws Exception {
    var headers = defaultHeaders();
    ConsortiumEntity consortiumEntity = createConsortiumEntity("111841e3-e6fb-4191-8fd8-5674a5107c33", "Test");
    List<ConsortiumEntity> consortiumEntityList = new ArrayList<>();
    consortiumEntityList.add(consortiumEntity);

    when(consortiumRepository.findAll(PageRequest.of(0, 1)))
      .thenReturn(new PageImpl<>(consortiumEntityList, PageRequest.of(0, 1), consortiumEntityList.size()));
    this.mockMvc.perform(
        get("/consortia")
          .headers(headers))
      .andExpect(matchAll(status().isOk()));
  }

  private ConsortiumEntity createConsortiumEntity(String id, String name) {
    ConsortiumEntity consortiumEntity = new ConsortiumEntity();
    consortiumEntity.setId(UUID.fromString(id));
    consortiumEntity.setName(name);
    return consortiumEntity;
  }
}
