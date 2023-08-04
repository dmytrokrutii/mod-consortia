package org.folio.consortia.config;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.folio.consortia.config.FolioExecutionContextHelper.AUTHTOKEN_REFRESH_CACHE_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.consortia.support.BaseIT;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import java.util.List;

class FolioExecutionContextHelperTest extends BaseIT {

  @Autowired
  private FolioExecutionContextHelper contextHelper;

  private static final String SYSTEM_USER = """
    {
      "users": [
        {
          "username": "consortia-system-user",
          "id": "a85c45b7-d427-4122-8532-5570219c5e59",
          "active": true,
          "departments": [],
          "proxyFor": [],
          "personal": {
            "addresses": []
          },
          "createdDate": "2021-03-17T15:30:07.106+00:00",
          "updatedDate": "2021-03-17T15:30:07.106+00:00",
          "metadata": {
            "createdDate": "2021-03-17T15:21:26.064+00:00",
            "updatedDate": "2021-03-17T15:30:07.043+00:00"
          }
        }
      ],
      "totalRecords": 1,
      "resultInfo": {
        "totalRecords": 1,
        "facets": [],
        "diagnostics": []
      }
    }
    """;

  @Test
  void shouldGetFolioExecutionContext() {
    // request to get token for 'consortia-system-user'
    wireMockServer.stubFor(
      post(urlEqualTo("/authn/login"))
        .willReturn(aResponse()
          .withHeader(XOkapiHeaders.TOKEN, TOKEN)));

    // request to get list of users by 'username' (='consortia-system-user')
    wireMockServer.stubFor(
      get(urlEqualTo("/users?query=username%3D%3Dconsortia-system-user"))
        .willReturn(aResponse()
          .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
          .withBody(SYSTEM_USER)));

    // 'execution context' should be created according to 'consortia-system-user' headers
    FolioExecutionContext executionContext = contextHelper.getSystemUserFolioExecutionContext(TENANT);

    assertEquals(TENANT, executionContext.getTenantId());
    assertEquals(wireMockServer.baseUrl(), executionContext.getOkapiUrl());
    assertEquals(TOKEN, executionContext.getToken());
    assertEquals("a85c45b7-d427-4122-8532-5570219c5e59", executionContext.getUserId().toString());
  }

  @Test
  void shouldGetFolioExecutionContextWithCustomOkapiHeaders() {
    // request to get token for 'consortia-system-user'
    wireMockServer.stubFor(
      post(urlEqualTo("/authn/login"))
        .willReturn(aResponse()
          .withHeader(XOkapiHeaders.TOKEN, TOKEN)));

    // request to get list of users by 'username' (='consortia-system-user')
    wireMockServer.stubFor(
      get(urlEqualTo("/users?query=username%3D%3Dconsortia-system-user"))
        .willReturn(aResponse()
          .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
          .withBody(SYSTEM_USER)));
    var headers = contextHelper.getHeadersForSystemUserWithRefreshPermissions(TENANT);

    // 'execution context' should be created according to 'consortia-system-user' headers
    FolioExecutionContext executionContext = contextHelper.getSystemUserFolioExecutionContext(TENANT, headers);

    assertEquals(TENANT, executionContext.getTenantId());
    Object refreshPermsVal = ((List)executionContext.getAllHeaders().get(AUTHTOKEN_REFRESH_CACHE_HEADER)).get(0);
    assertEquals(Boolean.TRUE.toString(), refreshPermsVal);
    assertEquals(wireMockServer.baseUrl(), executionContext.getOkapiUrl());
    assertEquals(TOKEN, executionContext.getToken());
    assertEquals("a85c45b7-d427-4122-8532-5570219c5e59", executionContext.getUserId().toString());
  }

  @Test
  void shouldGetExceptionWhenThereIsNoToken() {
    // request to get response without 'XOkapiHeaders.TOKEN'
    wireMockServer.stubFor(
      post(urlEqualTo("/authn/login"))
        .willReturn(aResponse()));

    // should get exception: verify exception type and message
    Exception exception = assertThrows(IllegalStateException.class, () -> contextHelper.getSystemUserFolioExecutionContext(TENANT));
    assertEquals(String.format("Cannot create FolioExecutionContext for Tenant: %s because of absent token", TENANT), exception.getMessage());
  }
}
