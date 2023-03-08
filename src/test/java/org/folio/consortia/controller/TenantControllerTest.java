package org.folio.consortia.controller;

import org.folio.consortia.support.BaseTest;
import org.folio.consortia.repository.entity.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EntityScan(basePackageClasses = Tenant.class)
class TenantControllerTest extends BaseTest {
  @Test
  void getTenants() throws Exception {
    var headers = defaultHeaders();
    this.mockMvc.perform(get("/consortia/tenants?limit=2&offset=1").headers(headers))
      .andExpect(
        matchAll(
          status().isOk(),
          content().contentType(MediaType.APPLICATION_JSON_VALUE)));
  }
  @Test
  void getBadRequest() throws Exception {
    var headers = defaultHeaders();
    this.mockMvc.perform(get("/consortia/tenants?limit=0&offset=0").headers(headers))
      .andExpect(
        matchAll(
          status().is4xxClientError(),
          content().contentType(MediaType.TEXT_PLAIN+";charset=UTF-8"),
          jsonPath("$", is("Limit cannot be negative or zero: 0"))));
  }
}
