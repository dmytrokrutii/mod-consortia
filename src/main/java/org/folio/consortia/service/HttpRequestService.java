package org.folio.consortia.service;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public interface HttpRequestService {
  ResponseEntity<String> performRequest(String url, HttpMethod httpMethod, Object payload);
}
