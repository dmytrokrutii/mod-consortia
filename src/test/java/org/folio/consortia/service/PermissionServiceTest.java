package org.folio.consortia.service;

import org.folio.consortia.client.PermissionsClient;
import org.folio.consortia.domain.dto.PermissionUser;
import org.folio.consortia.service.impl.PermissionUserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@EnableAutoConfiguration(exclude = BatchAutoConfiguration.class)
class PermissionServiceTest {
  private static final String PERMISSIONS_FILE_PATH = "permissions/test-user-permissions.csv";
  private static final String EMPTY_PERMISSIONS_FILE_PATH = "permissions/test-user--empty-permissions.csv";
  @InjectMocks
  PermissionUserServiceImpl permissionUserService;
  @Mock
  PermissionsClient permissionsClient;

  @Test
  void shouldThrowErrorForEmptyPermissionFileWhileAdding() {
    PermissionUser permissionUser = PermissionUser.of(UUID.randomUUID().toString(), UUID.randomUUID().toString(), List.of());

    Assertions.assertThrows(java.lang.IllegalStateException.class, () -> permissionUserService.addPermissions(permissionUser, EMPTY_PERMISSIONS_FILE_PATH));
  }

  @Test
  void shouldThrowErrorForEmptyPermissionFileWhileCreating() {
    Assertions.assertThrows(java.lang.IllegalStateException.class, () -> permissionUserService.createWithPermissionsFromFile(UUID.randomUUID().toString(), EMPTY_PERMISSIONS_FILE_PATH));
  }

  @Test
  void shouldAddPermissionsToPermissionUser() {
    PermissionUser permissionUser = PermissionUser.of(UUID.randomUUID().toString(), UUID.randomUUID().toString(), List.of());

    Mockito.doNothing().when(permissionsClient).addPermission(any(),any());
    Assertions.assertDoesNotThrow(() -> permissionUserService.addPermissions(permissionUser, PERMISSIONS_FILE_PATH));
  }
}
