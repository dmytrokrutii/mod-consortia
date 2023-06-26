package org.folio.consortia.controller;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.UUID;

import org.folio.consortia.domain.dto.SharingInstance;
import org.folio.consortia.rest.resource.SharingApi;
import org.folio.consortia.service.SharingInstanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/consortia/{consortiumId}")
@Log4j2
@RequiredArgsConstructor
public class SharingInstanceController implements  SharingApi{

  private final SharingInstanceService sharingInstanceService;

  @Override
  public ResponseEntity<SharingInstance> start(UUID consortiumId, @Validated SharingInstance sharingInstance) {
    return ResponseEntity.status(CREATED).body(sharingInstanceService.start(consortiumId, sharingInstance));
  }

  @Override
  public ResponseEntity<SharingInstance> getSharingInstanceById(UUID consortiumId, UUID actionId) {
    return ResponseEntity.ok(sharingInstanceService.getById(consortiumId, actionId));
  }
}
