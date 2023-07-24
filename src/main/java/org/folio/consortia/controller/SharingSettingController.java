package org.folio.consortia.controller;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.UUID;

import org.folio.consortia.domain.dto.SharingSettingRequest;
import org.folio.consortia.domain.dto.SharingSettingResponse;
import org.folio.consortia.rest.resource.SettingsApi;
import org.folio.consortia.service.SharingSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/consortia/{consortiumId}/sharing")
@RequiredArgsConstructor
public class SharingSettingController implements SettingsApi {

  private final SharingSettingService sharingSettingService;

  @Override
  public ResponseEntity<SharingSettingResponse> startSharingSetting(UUID consortiumId, SharingSettingRequest sharingSettingRequest) {
    return ResponseEntity.status(CREATED).body(sharingSettingService.start(consortiumId, sharingSettingRequest));
  }
}
