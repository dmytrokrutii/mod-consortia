package org.folio.consortia.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.consortia.client.UsersClient;
import org.folio.consortia.domain.dto.Personal;
import org.folio.consortia.domain.dto.User;
import org.folio.consortia.exception.ConsortiumClientException;
import org.folio.consortia.exception.ResourceNotFoundException;
import org.folio.consortia.service.UserService;
import org.folio.consortia.utils.HelperUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.folio.consortia.utils.TenantContextUtils.prepareContextForTenant;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  public static final String PATRON_GROUP = null;
  private static final String USER_ID = "userId";

  private final UsersClient usersClient;
  private final FolioExecutionContext folioExecutionContext;
  private final FolioModuleMetadata folioModuleMetadata;
  private static final Integer RANDOM_STRING_COUNT = 5;

  @Override
  public User createUser(User user) {
    log.info("Creating user with id {}.", user.getId());
    usersClient.saveUser(user);
    return user;
  }

  public void updateUser(User user) {
    log.info("Updating User '{}'.", user.getId());
    usersClient.updateUser(user.getId(), user);
  }

  @Override
  public User getById(UUID userId) {
    try {
      log.info("Getting user by userId {}.", userId);
      return usersClient.getUsersByUserId(String.valueOf(userId));
    } catch (FeignException.NotFound e) {
      log.info("User with userId {} does not exist in schema, going to use new one", userId);
      return new User();
    } catch (FeignException.Forbidden e) {
      throw new ConsortiumClientException(e);
    } catch (FeignException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Optional<User> getByUsername(String username) {
    return usersClient.getUsersByQuery("username==" + username)
      .getUsers()
      .stream()
      .findFirst();
  }

  @Override
  public void deleteById(String userId) {
    usersClient.deleteUser(userId);
  }

  public User prepareShadowUser(UUID userId, String tenantId) {
    try (var context = new FolioExecutionContextSetter(prepareContextForTenant(tenantId, folioModuleMetadata, (FolioExecutionContext) folioExecutionContext.getInstance()))) {
      User user = new User();
      User userOptional = usersClient.getUsersByUserId(userId.toString());

      if (Objects.nonNull(userOptional.getId())) {
        user.setId(userId.toString());
        user.setPatronGroup(PATRON_GROUP);
        user.setUsername(userOptional.getUsername() + HelperUtils.randomString(RANDOM_STRING_COUNT));
        var userPersonal = userOptional.getPersonal();
        if (Objects.nonNull(userPersonal)) {
          Personal personal = new Personal();
          personal.setLastName(userPersonal.getLastName());
          personal.setFirstName(userPersonal.getFirstName());
          personal.setEmail(userPersonal.getEmail());
          personal.setPreferredContactTypeId(userPersonal.getPreferredContactTypeId());
          user.setPersonal(personal);
        }
        user.setActive(true);
      } else {
        log.warn("Could not find real user with id: {} in his home tenant: {}", userId.toString(), tenantId);
        throw new ResourceNotFoundException(USER_ID, userId.toString());
      }
      return user;
    }
  }

}
