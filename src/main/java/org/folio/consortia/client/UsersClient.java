package org.folio.consortia.client;

import org.folio.consortia.domain.dto.User;
import org.folio.consortia.domain.dto.UserCollection;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "consortia-users-client", url = "users" , configuration = FeignClientConfiguration.class)
public interface UsersClient {

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  UserCollection getUserCollection(@RequestParam String query, @RequestParam int offset, @RequestParam int limit);

  @GetMapping
  UserCollection getUsersByQuery(@RequestParam("query") String query);

  @GetMapping(value = "/{userId}")
  User getUsersByUserId(@PathVariable String userId);

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  void saveUser(@RequestBody User user);

  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  void updateUser(@PathVariable String id, @RequestBody User user);

  @DeleteMapping(value = "/{userId}")
  void deleteUser(@PathVariable String userId);
}
