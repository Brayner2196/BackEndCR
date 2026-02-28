package com.backendcr.residentialcomplex.tenant.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backendcr.residentialcomplex.entity.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

	Optional<Tenant> findBySchemaName(String schemaName);

	boolean existsBySchemaName(String string);

}
