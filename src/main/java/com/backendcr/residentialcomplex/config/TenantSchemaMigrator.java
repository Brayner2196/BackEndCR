package com.backendcr.residentialcomplex.config;

import com.backendcr.residentialcomplex.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migrador incremental de schemas de tenants existentes.
 *
 * Ejecuta CREATE TABLE IF NOT EXISTS para tablas nuevas agregadas después
 * de la creación inicial del tenant. Es idempotente y seguro en cada arranque.
 *
 * Order(2) para correr DESPUÉS del DataInitializer (Order default = MAX).
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class TenantSchemaMigrator implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Iniciando migración incremental de schemas de tenants...");
        tenantRepository.findAll().forEach(tenant -> {
            String schema = tenant.getSchemaName();
            try {
                migrarMiembroConsejo(schema);
            } catch (Exception e) {
                log.error("Error migrando schema '{}': {}", schema, e.getMessage());
            }
        });
        log.info("Migración de schemas completada.");
    }

    // ─── Migraciones ──────────────────────────────────────────────────────────

    private void migrarMiembroConsejo(String schema) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.miembro_consejo (
                    id           BIGSERIAL   PRIMARY KEY,
                    usuario_id   BIGINT      NOT NULL REFERENCES %s.usuarios(id),
                    cargo        VARCHAR(20) NOT NULL,
                    fecha_inicio DATE        NOT NULL,
                    fecha_fin    DATE,
                    activo       BOOLEAN     NOT NULL DEFAULT TRUE,
                    creado_en    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema));
        log.debug("miembro_consejo verificada/creada en schema '{}'", schema);
    }
}
