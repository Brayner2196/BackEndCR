package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Resident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResidentRepository extends JpaRepository<Resident, Long> {
    
    List<Resident> findByTenantId(String tenantId);
    
    Optional<Resident> findByIdAndTenantId(Long id, String tenantId);
    
    List<Resident> findByUnitIdAndTenantId(Long unitId, String tenantId);
    
    List<Resident> findByIsOwnerAndTenantId(Boolean isOwner, String tenantId);
    
    List<Resident> findByLastNameContainingIgnoreCaseAndTenantId(String lastName, String tenantId);
}
