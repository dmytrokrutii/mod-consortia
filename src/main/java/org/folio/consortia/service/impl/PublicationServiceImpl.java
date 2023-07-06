package org.folio.consortia.service.impl;

import static org.folio.consortia.exception.PublicationException.PRIMARY_AFFILIATION_NOT_EXISTS;
import static org.folio.consortia.exception.PublicationException.TENANT_LIST_EMPTY;
import static org.folio.consortia.utils.TenantContextUtils.prepareContextForTenant;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HttpException;
import org.folio.consortia.domain.dto.PublicationRequest;
import org.folio.consortia.domain.dto.PublicationResponse;
import org.folio.consortia.domain.dto.PublicationStatus;
import org.folio.consortia.domain.entity.PublicationStatusEntity;
import org.folio.consortia.domain.entity.PublicationTenantRequestEntity;
import org.folio.consortia.exception.PublicationException;
import org.folio.consortia.repository.PublicationStatusRepository;
import org.folio.consortia.repository.PublicationTenantRequestRepository;
import org.folio.consortia.service.HttpRequestService;
import org.folio.consortia.service.PublicationService;
import org.folio.consortia.service.TenantService;
import org.folio.consortia.service.UserTenantService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class PublicationServiceImpl implements PublicationService {
  private final TenantService tenantService;
  private final UserTenantService userTenantService;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;
  private final HttpRequestService httpRequestService;
  private final TaskExecutor asyncTaskExecutor;

  private final PublicationStatusRepository publicationStatusRepository;
  private final PublicationTenantRequestRepository publicationTenantRequestRepository;
  private final ObjectMapper objectMapper;
  private static final int MAX_ACTIVE_THREADS = 5;

  @Override
  @SneakyThrows
  public PublicationResponse publishRequest(UUID consortiumId, PublicationRequest publicationRequest) {
    validatePublicationRequest(consortiumId, publicationRequest, folioExecutionContext);

    PublicationStatusEntity createdPublicationEntity = savePublicationStatusRecord(publicationRequest.getTenants().size());

    asyncTaskExecutor.execute(() -> processTenantRequests(publicationRequest, createdPublicationEntity));

    return buildPublicationResponse(createdPublicationEntity.getId());
  }
  void processTenantRequests(PublicationRequest publicationRequest, PublicationStatusEntity createdPublicationEntity) {
    List<CompletableFuture<PublicationTenantRequestEntity>> futures = new ArrayList<>();
    Semaphore semaphore = new Semaphore(MAX_ACTIVE_THREADS);

    for (String tenantId : publicationRequest.getTenants()) {
      try {
        PublicationTenantRequestEntity ptrEntity = buildPublicationRequestEntity(publicationRequest, createdPublicationEntity, tenantId);

        var savedPublicationTenantRequest = savePublicationTenantRequest(ptrEntity, folioExecutionContext);

        semaphore.acquire();
        var future = executeAsyncHttpRequest(publicationRequest, tenantId, folioExecutionContext)
          .whenComplete((ok, err) -> semaphore.release())
          .handle((response, t) -> updatePublicationTenantRequest(response, t, savedPublicationTenantRequest, folioExecutionContext));
        futures.add(future);
      } catch (RuntimeException | JsonProcessingException e) {
        log.error("publishRequest:: failed to save publication tenant request", e);
        semaphore.release();
        futures.add(CompletableFuture.failedFuture(e));
      } catch (InterruptedException ie) {
        log.error("publishRequest:: failed to acquire semaphore permit", ie);
        futures.add(CompletableFuture.failedFuture(ie));
        Thread.currentThread().interrupt();
      }
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
      .thenAccept(cf -> updatePublicationsStatus(futures, createdPublicationEntity, folioExecutionContext));
  }

  private PublicationTenantRequestEntity savePublicationTenantRequest(PublicationTenantRequestEntity ptrEntity, FolioExecutionContext centralTenantContext) {
    try (var context = new FolioExecutionContextSetter(prepareContextForTenant(centralTenantContext.getTenantId(), folioModuleMetadata, centralTenantContext))) {
      return publicationTenantRequestRepository.save(ptrEntity);
    } catch (RuntimeException e) {
      log.error("savePublicationTenantRequest:: error saving publication tenant request {}", ptrEntity.getId(), e);
      throw new PublicationException(e);
    }
  }

  CompletableFuture<ResponseEntity<String>> executeAsyncHttpRequest(PublicationRequest publicationRequest, String tenantId, FolioExecutionContext centralTenantContext) {
    return CompletableFuture.supplyAsync(() -> {
        try (var context = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, centralTenantContext))) {
          var response = httpRequestService.performRequest(publicationRequest.getUrl(), HttpMethod.POST, publicationRequest.getPayload());
          if (response.getStatusCode().is2xxSuccessful()) {
            log.info("executeAsyncTask:: successfully called {} on tenant {}", publicationRequest.getUrl(), tenantId);
          } else {
            var errMessage = response.getBody() != null ? response.getBody() : "Generic Error";
            log.error("executeAsyncTask:: error making {} '{}' request on tenant {}", publicationRequest.getMethod(), publicationRequest.getUrl(), tenantId, new HttpException(errMessage));
            throw new HttpClientErrorException(response.getStatusCode(), response.getBody());
          }
          return response;
        } catch (HttpClientErrorException e) {
          log.error("executeAsyncTask:: error making {} '{}' request on tenant {}", publicationRequest.getMethod(), publicationRequest.getUrl(), tenantId, new HttpException(e.getMessage()));
          throw new HttpClientErrorException(e.getStatusCode(), e.getMessage());
        }
      });
  }

  @SneakyThrows
  PublicationTenantRequestEntity updatePublicationTenantRequest(ResponseEntity<String> responseEntity, Throwable t,
      PublicationTenantRequestEntity ptrEntity, FolioExecutionContext centralTenantContext) {

      var currentLocalDateTime = LocalDateTime.now();
      ptrEntity.setCompletedDate(currentLocalDateTime);

      if (t == null) {
        ptrEntity.setResponseStatusCode(responseEntity.getStatusCode().value());
        ptrEntity.setResponse(responseEntity.getBody());
        ptrEntity.setStatus(PublicationStatus.COMPLETE);
      } else {
        ptrEntity.setStatus(PublicationStatus.ERROR);
        if (t.getCause() instanceof HttpClientErrorException httpClientErrorException) {
          ptrEntity.setResponseStatusCode(httpClientErrorException.getStatusCode().value());
          ptrEntity.setResponse(httpClientErrorException.getStatusText());
        } else {
          ptrEntity.setResponseStatusCode(HttpStatus.BAD_REQUEST.value());
          ptrEntity.setResponse(t.getMessage());
        }
      }

      return savePublicationTenantRequest(ptrEntity, centralTenantContext);
  }

  private PublicationTenantRequestEntity buildPublicationRequestEntity(PublicationRequest publicationRequest,
      PublicationStatusEntity savedPublicationEntity, String tenantId) throws JsonProcessingException {

    PublicationTenantRequestEntity ptrEntity = new PublicationTenantRequestEntity();
    String payload = objectMapper.writeValueAsString(publicationRequest.getPayload());

    ptrEntity.setId(UUID.randomUUID());
    ptrEntity.setRequestUrl(publicationRequest.getUrl());
    ptrEntity.setRequestPayload(payload);
    ptrEntity.setTenantId(tenantId);
    ptrEntity.setStatus(PublicationStatus.IN_PROGRESS);
    ptrEntity.setPcState(savedPublicationEntity);

    return ptrEntity;
  }

  private PublicationStatusEntity savePublicationStatusRecord(int totalRecords) {
    PublicationStatusEntity publicationStatusEntity = new PublicationStatusEntity();
    publicationStatusEntity.setId(UUID.randomUUID());
    publicationStatusEntity.setStatus(PublicationStatus.IN_PROGRESS);
    publicationStatusEntity.setTotalRecords(totalRecords);

    var savedPSE = publicationStatusRepository.save(publicationStatusEntity);

    log.info("Created publication record {}", savedPSE.getId());
    return savedPSE;
  }

  private void updatePublicationsStatus(List<CompletableFuture<PublicationTenantRequestEntity>> futures, PublicationStatusEntity publicationStatusEntity, FolioExecutionContext centralTenantContext) {
    var isCompletedWithExceptions = futures.stream().anyMatch(CompletableFuture::isCompletedExceptionally);
    var hasErrorStatus = futures.stream()
      .filter(future -> !future.isCompletedExceptionally())
      .map(CompletableFuture::join)
      .map(PublicationTenantRequestEntity::getStatus)
      .anyMatch(status -> status.equals(PublicationStatus.ERROR));
    var isErrorStatus = isCompletedWithExceptions || hasErrorStatus || futures.isEmpty();

    var updateStatus = isErrorStatus ? PublicationStatus.ERROR : PublicationStatus.COMPLETE;
    publicationStatusEntity.setStatus(updateStatus);

    try (var context = new FolioExecutionContextSetter(prepareContextForTenant(centralTenantContext.getTenantId(), folioModuleMetadata, centralTenantContext))) {
      publicationStatusRepository.save(publicationStatusEntity);
    }
    log.info("Updated publication record {} with status {}", publicationStatusEntity.getId(), publicationStatusEntity.getStatus());
  }

  private void validatePublicationRequest(UUID consortiumId, PublicationRequest publication, FolioExecutionContext context) {
    if (CollectionUtils.isEmpty(publication.getTenants())) {
      throw new PublicationException(TENANT_LIST_EMPTY);
    }
    tenantService.checkTenantsAndConsortiumExistsOrThrow(consortiumId, List.copyOf(publication.getTenants()));
    var userAffiliated = userTenantService.checkUserIfHasPrimaryAffiliationByUserId(consortiumId, context.getUserId().toString());
    if (!userAffiliated) {
      throw new PublicationException(PRIMARY_AFFILIATION_NOT_EXISTS);
    }
  }

  private PublicationResponse buildPublicationResponse(UUID publicationId) {
    return new PublicationResponse()
      .id(publicationId.toString())
      .status(PublicationStatus.IN_PROGRESS);
  }

}
