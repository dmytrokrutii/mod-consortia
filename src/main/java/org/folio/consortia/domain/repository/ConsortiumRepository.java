package org.folio.consortia.domain.repository;

import org.folio.consortia.domain.entity.ConsortiumEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsortiumRepository extends JpaRepository<ConsortiumEntity, UUID> {
}
