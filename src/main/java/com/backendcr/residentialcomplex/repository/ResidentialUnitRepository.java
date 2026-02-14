package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.ResidentialUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResidentialUnitRepository extends JpaRepository<ResidentialUnit, Long> {
    
    List<ResidentialUnit> findByTenantId(String tenantId);
    
    Optional<ResidentialUnit> findByIdAndTenantId(Long id, String tenantId);
    
    List<ResidentialUnit> findByComplexIdAndTenantId(Long complexId, String tenantId);
    
    List<ResidentialUnit> findByIsOccupiedAndTenantId(Boolean isOccupied, String tenantId);
}
