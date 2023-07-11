package org.folio.consortia.controller;

import java.util.UUID;

import org.folio.consortia.domain.dto.PublicationDetailsResponse;
import org.folio.consortia.domain.dto.PublicationRequest;
import org.folio.consortia.domain.dto.PublicationResponse;
import org.folio.consortia.domain.dto.PublicationResultCollection;
import org.folio.consortia.service.PublicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/consortia/{consortiumId}")
@RequiredArgsConstructor
public class PublicationController implements org.folio.consortia.rest.resource.PublicationsApi {

  private final PublicationService publishCoordinatorService;

  @Override
  public ResponseEntity<PublicationResponse> publishRequests(UUID consortiumId, PublicationRequest publicationRequest) {
    return ResponseEntity.ok(publishCoordinatorService.publishRequest(consortiumId, publicationRequest));
  }

  @Override
  public ResponseEntity<PublicationDetailsResponse> getPublicationDetails(UUID consortiumId, UUID publicationId){
    return ResponseEntity.ok(publishCoordinatorService.getPublicationDetails(consortiumId, publicationId));
  }

  @Override
  public ResponseEntity<PublicationResultCollection> getPublicationResults(UUID consortiumId, UUID publicationId) {
    return ResponseEntity.ok(publishCoordinatorService.getPublicationResults(consortiumId, publicationId));
  }
}
