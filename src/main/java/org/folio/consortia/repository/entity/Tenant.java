package org.folio.consortia.repository.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Tenant {
  @Id
  private String id;
  private String name;
}
