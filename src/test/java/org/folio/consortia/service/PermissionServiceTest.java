package org.folio.consortia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.consortia.utils.EntityUtils.RANDOM_USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.folio.consortia.client.PermissionsClient;
import org.folio.consortia.domain.dto.PermissionUser;
import org.folio.consortia.service.impl.PermissionUserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

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
    PermissionUser permissionUser = PermissionUser.of(UUID.randomUUID().toString(), RANDOM_USER_ID, List.of());

    Assertions.assertThrows(java.lang.IllegalStateException.class, () -> permissionUserService.addPermissions(permissionUser, EMPTY_PERMISSIONS_FILE_PATH));
  }

  @Test
  void shouldThrowErrorForEmptyPermissionFileWhileCreating() {
    Assertions.assertThrows(java.lang.IllegalStateException.class, () -> permissionUserService.createWithPermissionsFromFile(UUID.randomUUID().toString(), EMPTY_PERMISSIONS_FILE_PATH));
  }

  @Test
  void shouldAddPermissionsToPermissionUser() {
    PermissionUser permissionUser = PermissionUser.of(UUID.randomUUID().toString(), RANDOM_USER_ID, List.of());

    doNothing().when(permissionsClient).addPermission(any(),any());
    Assertions.assertDoesNotThrow(() -> permissionUserService.addPermissions(permissionUser, PERMISSIONS_FILE_PATH));
  }

  @Test
  void shouldCreateEmptyPermission() {
    String userId = RANDOM_USER_ID;
    ArgumentCaptor<PermissionUser> captor = ArgumentCaptor.forClass(PermissionUser.class);
    permissionUserService.createWithEmptyPermissions(userId);
    verify(permissionsClient).create(captor.capture());
    PermissionUser created = captor.getValue();
    assertThat(created.getUserId()).isEqualTo(userId);
    assertThat(created.getPermissions()).isEmpty();
  }

  @Test
  void shouldDeletePermissionUser() {
    String permissionUserId = UUID.randomUUID().toString();
    permissionUserService.deletePermissionUser(permissionUserId);
    verify(permissionsClient).deletePermissionUser(permissionUserId);
  }

}
