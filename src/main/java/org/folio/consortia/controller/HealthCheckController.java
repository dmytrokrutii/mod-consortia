package org.folio.consortia.controller;

import org.folio.pv.rest.resource.HealthCheckApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/consortia")
public class HealthCheckController implements HealthCheckApi {

  @Override
  public ResponseEntity<Void> healthCheckGet() {
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
