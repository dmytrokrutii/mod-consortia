package org.folio.consortia.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserCollection {

  private List<User> users;
  private Integer totalRecords;

}
