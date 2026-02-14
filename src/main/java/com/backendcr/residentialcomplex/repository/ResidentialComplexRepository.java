package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.ResidentialComplex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResidentialComplexRepository extends JpaRepository<ResidentialComplex, Long> {
    
    List<ResidentialComplex> findByTenantId(String tenantId);
    
    Optional<ResidentialComplex> findByIdAndTenantId(Long id, String tenantId);
    
    List<ResidentialComplex> findByNameContainingIgnoreCaseAndTenantId(String name, String tenantId);
}
