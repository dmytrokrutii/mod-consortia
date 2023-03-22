package org.folio.consortia.controller;

import org.folio.consortia.domain.dto.UserTenant;
import org.folio.consortia.domain.dto.UserTenantCollection;
import org.folio.consortia.domain.entity.ConsortiumEntity;
import org.folio.consortia.domain.entity.UserTenantEntity;
import org.folio.consortia.domain.repository.ConsortiumRepository;
import org.folio.consortia.service.UserTenantService;
import org.folio.consortia.support.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EntityScan(basePackageClasses = UserTenantEntity.class)
class UserTenantControllerTest extends BaseTest {

  @Mock
  private UserTenantService userTenantService;
  @InjectMocks
  private UserTenantController userTenantController;
  @MockBean
  private ConsortiumRepository consortiumRepository;

  @Test
  void shouldGetUserTenantsByUserId() {
    // given
    UUID userId = UUID.randomUUID();
    List<UserTenant> userTenantDtos = List.of(new UserTenant(), new UserTenant());
    UserTenantCollection userTenantCollection = new UserTenantCollection();
    userTenantCollection.setUserTenants(userTenantDtos);
    userTenantCollection.setTotalRecords(userTenantDtos.size());

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(createConsortiumEntity()));

    when(userTenantService.getByUserId(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), userId, 0, 10))
      .thenReturn(userTenantCollection);

    // when
    ResponseEntity<UserTenantCollection> response =
      userTenantController.getUserTenants(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), userId, null, null, 0, 10);

    // then
    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertEquals(userTenantCollection, response.getBody());

    verify(userTenantService).getByUserId(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), userId, 0, 10);
  }

  @Test
  void shouldGetUserTenantByAssociationId() {
    // given
    UUID associationId = UUID.randomUUID();
    UserTenant userTenant = new UserTenant();
    userTenant.setId(associationId);
    userTenant.setUserId(UUID.randomUUID());
    userTenant.setUsername("username");
    userTenant.setTenantId(String.valueOf(UUID.randomUUID()));
    userTenant.setIsPrimary(true);

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(createConsortiumEntity()));

    when(userTenantService.getById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), associationId))
      .thenReturn(userTenant);

    // when
    ResponseEntity<UserTenant> response = userTenantController.getUserTenantByAssociationId(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), associationId);

    // then
    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    Assertions.assertEquals(userTenant, response.getBody());

    verify(userTenantService).getById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"), associationId);
  }

  @Test
  void shouldGetUserTenantList() throws Exception {
    var headers = defaultHeaders();

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(createConsortiumEntity()));
    this.mockMvc.perform(get("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/user-tenants?limit=2&offset=1").headers(headers))
      .andExpectAll(
        status().isOk(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE));
  }

  @Test
  void return404UserTenantNotFoundByAssociationId() throws Exception {
    var headers = defaultHeaders();

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(createConsortiumEntity()));
    this.mockMvc.perform(get("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/user-tenants/cb28f43c-bf45-11ed-afa1-0242ac120002").headers(headers))
      .andExpectAll(
        status().is4xxClientError(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE),
        jsonPath("$.errors[0].code",
          is("NOT_FOUND_ERROR")));
  }

  @Test
  void return400BadRequest() throws Exception {
    var headers = defaultHeaders();

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(createConsortiumEntity()));
    this.mockMvc.perform(get("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/user-tenants?limit=0&offset=0").headers(headers))
      .andExpectAll(
        status().is4xxClientError(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE),
        jsonPath("$.errors[0].code",
          is("VALIDATION_ERROR")));
  }

  @Test
  void return404UserTenantNotFoundWithUserId() throws Exception {
    var headers = defaultHeaders();

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(createConsortiumEntity()));
    this.mockMvc.perform(get("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/user-tenants?userId=8ad4c4b4-4d4c-4bf9-a8a0-7a30c1edf34b").headers(headers))
      .andExpectAll(
        status().is4xxClientError(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE),
        jsonPath("$.errors[0].code",
          is("NOT_FOUND_ERROR")));
  }

  @Test
  void getValidationError() throws Exception {
    var headers = defaultHeaders();

    when(consortiumRepository.findById(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002")))
      .thenReturn(Optional.of(createConsortiumEntity()));
    this.mockMvc.perform(get("/consortia/7698e46-c3e3-11ed-afa1-0242ac120002/user-tenants?userId=90unnn").headers(headers))
      .andExpectAll(
        status().is4xxClientError(),
        content().contentType(MediaType.APPLICATION_JSON_VALUE),
        jsonPath("$.errors[0].code",
          is("VALIDATION_ERROR")));
  }

  private ConsortiumEntity createConsortiumEntity() {
    ConsortiumEntity consortiumEntity = new ConsortiumEntity();
    consortiumEntity.setId(UUID.fromString("7698e46-c3e3-11ed-afa1-0242ac120002"));
    consortiumEntity.setName("TestConsortium");
    return consortiumEntity;
  }
}
