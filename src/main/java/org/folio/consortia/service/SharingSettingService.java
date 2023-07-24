package org.folio.consortia.service;

import java.util.UUID;

import org.folio.consortia.domain.dto.SharingSettingRequest;
import org.folio.consortia.domain.dto.SharingSettingResponse;

public interface SharingSettingService {

  /**
   * Start sharing setting
   * @param consortiumId UUID of consortium entity
   * @param sharingSettingRequest the sharingSettingDTO (data transfer object)
   * @return SharingInstanceDto
   */
  SharingSettingResponse start(UUID consortiumId, SharingSettingRequest sharingSettingRequest);

}
