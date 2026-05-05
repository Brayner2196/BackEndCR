package com.backendcr.residentialcomplex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.backendcr.residentialcomplex.entity.Identidad;

public interface IdentidadRepository extends JpaRepository<Identidad, Long>{

	// Busca TODAS las identidades con ese email (puede haber varias en distintos tenants)
    List<Identidad> findAllByEmail(String email);

    // Busca identidad específica por email + tenant (para login con selección).
    // Usa query nativa con esquema explícito "public." para evitar que el search_path
    // del modo multi-tenant apunte a un esquema incorrecto.
    @Query(value = "SELECT * FROM public.identidades WHERE email = :email AND tenant_id = :tenantId", nativeQuery = true)
    Optional<Identidad> findByEmailAndTenantId(@Param("email") String email, @Param("tenantId") String tenantId);

    // Busca el super admin (tenant null)
    Optional<Identidad> findByEmailAndTenantIdIsNull(String email);

    boolean existsByEmailAndTenantId(String email, String tenantId);
}
