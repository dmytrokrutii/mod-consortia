package org.folio.consortia.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "tenant")
public class TenantEntity {
  @Id
  private String id;
  private String name;
  private UUID consortiumId;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TenantEntity that = (TenantEntity) o;
    return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(consortiumId, that.consortiumId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, consortiumId);
  }
}
