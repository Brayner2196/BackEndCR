package com.backendcr.residentialcomplex.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.entity.Identidad;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("default")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

	private final IdentidadRepository identidadRepository;
	private final JdbcTemplate jdbcTemplate;
	private final PasswordEncoder passwordEncoder;
	@Override
	public void run(String... args) {
		TenantContext.setTenant("public");
		log.info("Iniciando carga de datos de prueba...");

		crearTablasPublicas();
		crearSuperAdmin();

		log.info("Datos de prueba cargados correctamente");
		imprimirCredenciales();
	}

	// ─── Schema public ────────────────────────────────────────────────────────

	/**
	 * Crea las tablas del schema public que corresponden a las entidades
	 * anotadas con @Table(schema = "public"): identidades, tenants, device_tokens.
	 * Usa CREATE TABLE IF NOT EXISTS para ser idempotente en cada arranque.
	 */
	private void crearTablasPublicas() {
		log.info("Verificando tablas del schema public...");

		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS public.identidades (
				    id        BIGSERIAL PRIMARY KEY,
				    email     VARCHAR(255) NOT NULL,
				    password  VARCHAR(255) NOT NULL,
				    rol       VARCHAR(255) NOT NULL,
				    tenant_id VARCHAR(255)
				)
				""");

		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS public.tenants (
				    id          BIGSERIAL PRIMARY KEY,
				    schema_name VARCHAR(255) NOT NULL UNIQUE,
				    nombre      VARCHAR(255) NOT NULL,
				    codigo      VARCHAR(255) NOT NULL UNIQUE,
				    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
				    direccion   VARCHAR(255)
				)
				""");

		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS public.device_tokens (
				    id             BIGSERIAL PRIMARY KEY,
				    usuario_id     BIGINT       NOT NULL,
				    tenant_id      VARCHAR(255)  NOT NULL,
				    token          VARCHAR(512)  NOT NULL,
				    plataforma     VARCHAR(20)   NOT NULL,
				    actualizado_en TIMESTAMP,
				    UNIQUE(usuario_id, plataforma)
				)
				""");

		log.info("Tablas del schema public verificadas/creadas");
	}

	private void crearSuperAdmin() {
		TenantContext.setTenant("public");
		if (identidadRepository.findByEmailAndTenantIdIsNull("admin@app.com").isPresent()) {
			log.info("SUPER_ADMIN ya existe, omitiendo...");
			return;
		}

		Identidad admin = new Identidad();
		admin.setEmail("admin@app.com");
		admin.setPassword(passwordEncoder.encode("admin123"));
		admin.setRol("SUPER_ADMIN");
		admin.setTenantId(null);
		identidadRepository.save(admin);

		log.info("SUPER_ADMIN creado: admin@app.com / admin123");
	}

	private void imprimirCredenciales() {
		log.info("""

		    CREDENCIALES DE PRUEBA
		    
		    SUPER ADMIN:    admin@app.com / admin123
		    
		    """);
	}

}
