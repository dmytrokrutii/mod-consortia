package org.folio.consortia.service;

import static org.folio.consortia.utils.EntityUtils.createOkapiHeaders;
import static org.folio.consortia.utils.EntityUtils.createUserEntity;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.folio.consortia.client.UsersClient;
import org.folio.consortia.domain.dto.User;
import org.folio.consortia.domain.dto.UserType;
import org.folio.consortia.service.impl.UserServiceImpl;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    doNothing().when(usersClient).saveUser(user);
    User createdUser = userService.createUser(user);
    assertEquals(user, createdUser);
  }

  @Test
  void shouldUpdateUser() {
    User user = createUserEntity(false);
    doNothing().when(usersClient).updateUser(user.getId(), user);
    assertDoesNotThrow(() -> userService.updateUser(user));
  }

  @Test
  void shouldDeleteUser() {
    User user = createUserEntity(false);
    doNothing().when(usersClient).deleteUser(user.getId());
    assertDoesNotThrow(() -> userService.deleteById(user.getId()));
  }

  @Test
  void shouldThrowNotFoundWhilePrepareShadowUser() {
    when(usersClient.getUsersByUserId(any())).thenReturn(new User());
    mockOkapiHeaders();

    assertThrows(org.folio.consortia.exception.ResourceNotFoundException.class, () -> userService.prepareShadowUser(UUID.randomUUID(), ""));
  }

  @Test
  void shouldPrepareShadowUser() {
    when(usersClient.getUsersByUserId(any())).thenReturn(createUserEntity(true));
    mockOkapiHeaders();

    User user = userService.prepareShadowUser(UUID.randomUUID(), "diku");

    assertEquals(UserType.SHADOW.getName(), user.getType());
    assertEquals("diku", user.getCustomFields().get("originaltenantid"));
    assertEquals(true, user.getActive());
    assertEquals("testFirst", user.getPersonal().getFirstName());
    assertEquals("testLast", user.getPersonal().getLastName());
    assertEquals("Test@mail.com", user.getPersonal().getEmail());
    assertEquals("email", user.getPersonal().getPreferredContactTypeId());
  }

  private void mockOkapiHeaders() {
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    Map<String, Collection<String>> okapiHeaders = createOkapiHeaders();
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(okapiHeaders);
  }
}
