package org.folio.consortia.service;

import static org.folio.consortia.utils.EntityUtils.createUserEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.consortia.client.UsersClient;
import org.folio.consortia.domain.dto.User;
import org.folio.consortia.service.impl.UserServiceImpl;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnableAutoConfiguration(exclude = BatchAutoConfiguration.class)
class UserServiceTest {

  @InjectMocks
  UserServiceImpl userService;
  @Mock
  UsersClient usersClient;
  @Mock
  FolioModuleMetadata folioModuleMetadata;
  @Mock
  FolioExecutionContext folioExecutionContext;

  @Test
  void shouldCreateUser() {
    User user = createUserEntity(false);
    Mockito.doNothing().when(usersClient).saveUser(user);
    User createdUser = userService.createUser(user);
    Assertions.assertEquals(user, createdUser);
  }

  @Test
  void shouldUpdateUser() {
    User user = createUserEntity(false);
    Mockito.doNothing().when(usersClient).updateUser(user.getId(), user);
    Assertions.assertDoesNotThrow(() -> userService.updateUser(user));
  }

  @Test
  void shouldDeleteUser() {
    User user = createUserEntity(false);
    Mockito.doNothing().when(usersClient).deleteUser(user.getId());
    Assertions.assertDoesNotThrow(() -> userService.deleteById(user.getId()));
  }

  @Test
  void shouldThrowNotFoundWhilePrepareShadowUser() {
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);
    Mockito.when(usersClient.getUsersByUserId(any())).thenReturn(new User());
    Assertions.assertThrows(org.folio.consortia.exception.ResourceNotFoundException.class, () -> userService.prepareShadowUser(UUID.randomUUID(), ""));
  }

  @Test
  void shouldPrepareShadowUser() {
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    when(folioExecutionContext.getFolioModuleMetadata()).thenReturn(folioModuleMetadata);
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of("diku"));
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);
    Mockito.when(usersClient.getUsersByUserId(any())).thenReturn(createUserEntity(true));
    User user = userService.prepareShadowUser(UUID.randomUUID(), "diku");
    Assertions.assertEquals("shadow", user.getType());
    Assertions.assertEquals("diku", user.getCustomFields().get("originaltenantid"));
    Assertions.assertEquals(true, user.getActive());
    Assertions.assertEquals("testFirst", user.getPersonal().getFirstName());
    Assertions.assertEquals("testLast", user.getPersonal().getLastName());
  }

}
