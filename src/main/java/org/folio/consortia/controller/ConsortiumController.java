package org.folio.consortia.controller;

import lombok.RequiredArgsConstructor;
import org.folio.consortia.domain.dto.Consortium;
import org.folio.consortia.domain.dto.ConsortiumCollection;
import org.folio.consortia.rest.resource.ConsortiaApi;
import org.folio.consortia.service.ConsortiumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ConsortiumController implements ConsortiaApi {
  @Autowired
  ConsortiumService consortiumService;

  @Override
  @PostMapping("/consortia")
  public ResponseEntity<Consortium> saveConsortium(Consortium consortium) {
    return ResponseEntity.ok(consortiumService.save(consortium));
  }

  @Override
  @GetMapping("/consortia/{consortiumId}")
  public ResponseEntity<Consortium> getConsortium(UUID consortiumId) {
    return ResponseEntity.ok(consortiumService.get(consortiumId));
  }

  @Override
  @PutMapping("/consortia/{consortiumId}")
  public ResponseEntity<Consortium> updateConsortium(UUID consortiumId, Consortium consortium) {
    return ResponseEntity.ok(consortiumService.update(consortiumId, consortium));
  }

  @Override
  @GetMapping("/consortia")
  public ResponseEntity<ConsortiumCollection> getConsortiumCollection() {
    return ResponseEntity.ok(consortiumService.getAll());
  }
}
