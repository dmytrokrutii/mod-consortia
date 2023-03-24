package org.folio.consortia.service;

import org.folio.consortia.domain.dto.Consortium;
import org.folio.consortia.domain.dto.ConsortiumCollection;
import org.folio.consortia.domain.entity.ConsortiumEntity;

import java.util.UUID;

public interface ConsortiumService {

  /**
   * Inserts single consortium. It is prohibited to have more than consortium in the table.
   * All new consortia will be created in separate new consortia tenants and so in separate DB schemas.
   *
   * @param consortiumDto  the consortiumDto
   * @return consortiumDto
   */
  Consortium save(Consortium consortiumDto);

  /**
   * Gets consortium based on consortiumId.
   *
   * @param consortiumId  the consortiumId
   * @return consortiumDto
   */
  Consortium get(UUID consortiumId);

  /**
   * Updates single consortium based on consortiumId.
   *
   * @param consortiumId  the consortiumId
   * @param consortiumDto  the consortiumDto
   * @return consortiumDto
   */
  Consortium update(UUID consortiumId, Consortium consortiumDto);

  /**
   * Gets consortiums. Will returns only single record, because consortium table can persist only 1 consortium.
   * All new consortia will be created in separate new consortia tenants and so in separate DB schemas.
   *
   * @return consortiums collection
   */
  ConsortiumCollection getAll();

  ConsortiumEntity checkConsortiumExistsOrThrow(UUID consortiumId);
}
