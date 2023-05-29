package org.folio.consortia.security;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.consortia.client.AuthClient;
import org.folio.consortia.domain.dto.SystemUserParameters;
import org.folio.consortia.domain.dto.User;
import org.folio.consortia.service.UserService;
import org.folio.spring.integration.XOkapiHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class AuthService {

  private final AuthClient authClient;
  private final UserService userService;

  @Value("${folio.system.username}")
  private String username;

  @Value("${folio.system.password}")
  private String password;

  public String getTokenForSystemUser(String tenant, String url) {
    SystemUserParameters userParameters = SystemUserParameters.builder()
      .okapiUrl(url)
      .tenant(tenant)
      .username(username)
      .password(password)
      .build();

    log.info("Attempt login with url={} tenant={} username={}.", url, tenant, username);

    ResponseEntity<String> authResponse = authClient.getApiKey(userParameters);

    var token = authResponse.getHeaders().get(XOkapiHeaders.TOKEN);
    if (isNotEmpty(token)) {
      log.info("Logged in as {} in tenant {}", username, tenant);
      userParameters.setOkapiToken(token.get(0));
    } else {
      log.error("Can't get token logging in as {}.", username);
    }
    return userParameters.getOkapiToken();
  }

  public String getSystemUserId() {
    Optional<User> optionalUser = userService.getByUsername(username);

    if (optionalUser.isEmpty()) {
      log.error("Can't find user id by username {}.", username);
      return null;
    }
    return optionalUser.get().getId();
  }

  private boolean isNotEmpty(List<String> token) {
    return CollectionUtils.isNotEmpty(token) && StringUtils.isNotBlank(token.get(0));
  }

  public void saveCredentials(SystemUserParameters systemUserParameters) {
    authClient.saveCredentials(systemUserParameters);

    log.info("Saved credentials for user {}.", systemUserParameters.getUsername());
  }
}
