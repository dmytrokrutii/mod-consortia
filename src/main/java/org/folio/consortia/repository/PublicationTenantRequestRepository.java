package org.folio.consortia.repository;

import java.util.UUID;

import org.folio.consortia.domain.entity.PublicationTenantRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublicationTenantRequestRepository extends JpaRepository<PublicationTenantRequestEntity, UUID> {
}
