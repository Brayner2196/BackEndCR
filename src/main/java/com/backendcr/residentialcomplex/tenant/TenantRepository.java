package com.backendcr.residentialcomplex.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backendcr.residentialcomplex.entity.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
	
	Tenant findBySchemaName(String schemaName);

}
