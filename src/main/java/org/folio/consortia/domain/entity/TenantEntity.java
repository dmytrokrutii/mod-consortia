package org.folio.consortia.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

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

  @NotNull(message = "Invalid Code: Code is null")
  @Size(min = 3, max = 3, message = "Invalid Code length: Must be of 3 alphanumeric characters")
  @Pattern(regexp = "^[A-Za-z0-9]*$", message = "The code must be alphanumeric.")
  private String code;

  @NotBlank(message = "Invalid Name: Empty name")
  @NotNull(message = "Invalid Name: Name is NULL")
  @Size(min = 2, max = 150, message = "Invalid Name: Must be of 2 - 150 characters")
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
