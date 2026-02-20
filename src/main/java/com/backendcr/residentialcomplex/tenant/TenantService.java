package com.backendcr.residentialcomplex.tenant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.backendcr.residentialcomplex.entity.Tenant;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TenantService {

	private final TenantRepository tenantRepository;
    private final JdbcTemplate jdbcTemplate;
    
    public TenantService(TenantRepository tenantRepository, JdbcTemplate jdbcTemplate) {
		this.tenantRepository = tenantRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

    /**
     * Registra un nuevo tenant:
     * 1. Crea el schema en la BD
     * 2. Corre las migraciones en ese schema
     * 3. Guarda el tenant en la tabla maestra
     */
    public Tenant crearTenant(String schemaName, String nombre) {

        // 1. Crear schema
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

        // 2. Crear tablas en el nuevo schema (aquí llamas a Flyway manualmente
        //    o ejecutas el DDL directamente)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS %s.usuarios (
                id BIGSERIAL PRIMARY KEY,
                nombre VARCHAR(100),
                email VARCHAR(100) UNIQUE
            )
        """.formatted(schemaName));

        // 3. Guardar en tabla maestra
        Tenant tenant = new Tenant();
        tenant.setSchemaName(schemaName);
        tenant.setNombre(nombre);
        tenant.setActivo(true);
        return tenantRepository.save(tenant);
    }
}
