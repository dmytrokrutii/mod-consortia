package org.folio.consortia.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.consortia.domain.dto.UserTenant;
import org.folio.consortia.domain.dto.UserTenantCollection;
import org.folio.consortia.domain.entity.UserTenantEntity;
import org.folio.consortia.domain.repository.UserTenantRepository;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.folio.consortia.service.UserTenantService;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserTenantServiceImpl implements UserTenantService {

  private final UserTenantRepository userTenantRepository;
  private final ConversionService converter;

  @Override
  public UserTenantCollection get(Integer offset, Integer limit) {
    var result = new UserTenantCollection();
    Page<UserTenantEntity> userTenantPage = userTenantRepository.findAll(PageRequest.of(offset, limit));
    result.setUserTenants(userTenantPage.stream().map(o -> converter.convert(o, UserTenant.class)).toList());
    result.setTotalRecords((int) userTenantPage.getTotalElements());
    return result;
  }

  @Override
  public UserTenant getById(UUID id) {
    UserTenantEntity userTenantEntity = userTenantRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("id", String.valueOf(id)));
    return converter.convert(userTenantEntity, UserTenant.class);
  }

  @Override
  public UserTenantCollection getByUserId(UUID userId, Integer offset, Integer limit) {
    var result = new UserTenantCollection();
    Page<UserTenantEntity> userTenantPage = userTenantRepository.findByUserId(userId, PageRequest.of(offset, limit));

    if (userTenantPage.getContent().isEmpty()) {
      throw new ResourceNotFoundException("userId", String.valueOf(userId));
    }

    result.setUserTenants(userTenantPage.stream().map(o -> converter.convert(o, UserTenant.class)).toList());
    result.setTotalRecords((int) userTenantPage.getTotalElements());
    return result;
  }

  @Override
  public UserTenantCollection getByUsernameAndTenantId(String username, String tenantId) {
    var result = new UserTenantCollection();
    UserTenantEntity userTenantEntity = userTenantRepository.findByUsernameAndTenantId(username, tenantId)
      .orElseThrow(() -> new ResourceNotFoundException("username", username));
    UserTenant userTenant = converter.convert(userTenantEntity, UserTenant.class);

    result.setUserTenants(List.of(userTenant));
    result.setTotalRecords(1);
    return result;
  }

}
