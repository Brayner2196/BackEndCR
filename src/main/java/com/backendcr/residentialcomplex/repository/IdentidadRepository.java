package com.backendcr.residentialcomplex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backendcr.residentialcomplex.entity.Identidad;

public interface IdentidadRepository extends JpaRepository<Identidad, Long>{

	// Busca TODAS las identidades con ese email (puede haber varias en distintos tenants)
    List<Identidad> findAllByEmail(String email);

    // Busca identidad específica por email + tenant (para login con selección)
    Optional<Identidad> findByEmailAndTenantId(String email, String tenantId);

    // Busca el super admin (tenant null)
    Optional<Identidad> findByEmailAndTenantIdIsNull(String email);

    boolean existsByEmailAndTenantId(String email, String tenantId);
}
