package org.folio.consortia.domain.repository;

import org.folio.consortia.domain.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, String> {

}
