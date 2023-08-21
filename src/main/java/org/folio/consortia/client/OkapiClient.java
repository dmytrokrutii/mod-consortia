package org.folio.consortia.client;

import java.net.URI;

import com.fasterxml.jackson.databind.JsonNode;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "okapi", configuration = FeignClientConfiguration.class)
public interface OkapiClient {

  @GetMapping(value = "/proxy/tenants/{tenantId}/modules", produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getModuleIds(URI uri, @PathVariable("tenantId") String tenantId, @RequestParam("filter") String moduleName);
}
