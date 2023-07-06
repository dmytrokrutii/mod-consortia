package org.folio.consortia.service;

import java.util.UUID;

import org.folio.consortia.domain.dto.PublicationRequest;
import org.folio.consortia.domain.dto.PublicationResponse;

public interface PublicationService {
  PublicationResponse publishRequest(UUID consortiumId, PublicationRequest publication);

}
