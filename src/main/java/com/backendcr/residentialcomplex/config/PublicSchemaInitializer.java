package com.backendcr.residentialcomplex.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Inicializador del schema PUBLIC.
 *
 * Crea (si no existen) todas las tablas del schema public que corresponden a
 * entidades anotadas con @Table(schema = "public"): identidades, tenants,
 * device_tokens, refresh_tokens y tenant_pasarelas.
 *
 * A diferencia de {@link DataInitializer} (que solo corre en perfil "default"
 * para sembrar datos de prueba), este componente NO está atado a ningún perfil,
 * por lo que también se ejecuta en producción/Railway donde
 * spring.jpa.hibernate.ddl-auto=none. Así el despliegue crea las tablas base
 * automáticamente sin depender de Hibernate.
 *
 * Es idempotente (CREATE TABLE IF NOT EXISTS + ALTER ... ADD COLUMN IF NOT
 * EXISTS) y seguro en cada arranque. Order(0) para correr ANTES de
 * DataInitializer (siembra de datos) y TenantSchemaMigrator.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class PublicSchemaInitializer implements CommandLineRunner {

	private final JdbcTemplate jdbcTemplate;

	@Override
	public void run(String... args) {
		TenantContext.setTenant("public");
		try {
			log.info("Verificando tablas del schema public...");
			crearSchemaPublic();
			crearIdentidades();
			crearTenants();
			crearDeviceTokens();
			crearRefreshTokens();
			crearTenantPasarelas();
			log.info("Tablas del schema public verificadas/creadas");
		} finally {
			TenantContext.clear();
		}
	}

	// ─── Schema ────────────────────────────────────────────────────────────────

	private void crearSchemaPublic() {
		jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS public");
	}

	// ─── Tablas ────────────────────────────────────────────────────────────────

	private void crearIdentidades() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS public.identidades (
				    id        BIGSERIAL    PRIMARY KEY,
				    email     VARCHAR(255) NOT NULL,
				    password  VARCHAR(255) NOT NULL,
				    rol       VARCHAR(255) NOT NULL,
				    tenant_id VARCHAR(255),
				    activo    BOOLEAN      NOT NULL DEFAULT TRUE
				)
				""");
		// Auto-reparación para DBs creadas antes de agregar la columna.
		jdbcTemplate.execute(
				"ALTER TABLE public.identidades ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE");
	}

	private void crearTenants() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS public.tenants (
				    id          BIGSERIAL    PRIMARY KEY,
				    schema_name VARCHAR(255) NOT NULL UNIQUE,
				    nombre      VARCHAR(255) NOT NULL,
				    codigo      VARCHAR(255) NOT NULL UNIQUE,
				    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
				    direccion   VARCHAR(255),
				    timezone    VARCHAR(50)  NOT NULL DEFAULT 'America/Bogota'
				)
				""");
		jdbcTemplate.execute(
				"ALTER TABLE public.tenants ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) NOT NULL DEFAULT 'America/Bogota'");
	}

	private void crearDeviceTokens() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS public.device_tokens (
				    id             BIGSERIAL    PRIMARY KEY,
				    usuario_id     BIGINT       NOT NULL,
				    tenant_id      VARCHAR(255) NOT NULL,
				    token          VARCHAR(512) NOT NULL,
				    plataforma     VARCHAR(20)  NOT NULL,
				    actualizado_en TIMESTAMP,
				    UNIQUE(usuario_id, plataforma)
				)
				""");
	}

	private void crearRefreshTokens() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS public.refresh_tokens (
				    id           BIGSERIAL   PRIMARY KEY,
				    token        VARCHAR(64) NOT NULL UNIQUE,
				    identidad_id BIGINT      NOT NULL,
				    expires_at   TIMESTAMP   NOT NULL,
				    revoked      BOOLEAN     NOT NULL DEFAULT FALSE
				)
				""");
	}

	private void crearTenantPasarelas() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS public.tenant_pasarelas (
				    id             BIGSERIAL   PRIMARY KEY,
				    tenant_id      BIGINT      NOT NULL REFERENCES public.tenants(id),
				    tipo_pasarela  VARCHAR(50) NOT NULL,
				    activa         BOOLEAN     NOT NULL DEFAULT TRUE,
				    prioridad      INTEGER     NOT NULL DEFAULT 1,
				    sandbox        BOOLEAN     NOT NULL DEFAULT FALSE,
				    public_key     TEXT,
				    private_key    TEXT,
				    webhook_secret TEXT,
				    created_at     TIMESTAMP   NOT NULL,
				    updated_at     TIMESTAMP,
				    updated_by     VARCHAR(255),
				    success_url    TEXT,
				    failure_url    TEXT,
				    pending_url    TEXT,
				    UNIQUE(tenant_id, tipo_pasarela)
				)
				""");
	}
}
