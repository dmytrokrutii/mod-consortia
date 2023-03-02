package org.folio.consortia.controller;

import org.folio.consortia.support.BaseTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class HealthCheckControllerTest extends BaseTest {

  @Test
  void healthCheckGet() throws Exception {
    var headers = defaultHeaders();
    this.mockMvc.perform(get("/consortia/health-check").headers(headers))
      .andExpect(status().isOk());
  }
}
