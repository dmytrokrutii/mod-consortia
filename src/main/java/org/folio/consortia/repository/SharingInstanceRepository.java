package org.folio.consortia.repository;

import java.util.UUID;

import org.folio.consortia.domain.entity.SharingInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SharingInstanceRepository extends JpaRepository<SharingInstanceEntity, UUID> {

}
