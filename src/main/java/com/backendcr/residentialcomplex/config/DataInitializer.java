package com.backendcr.residentialcomplex.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.entity.Identidad;
import com.backendcr.residentialcomplex.entity.Tenant;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.tenant.repository.TenantRepository;
import com.backendcr.residentialcomplex.tenant.service.TenantService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

	private final IdentidadRepository identidadRepository;
	private final TenantRepository tenantRepository;
	private final JdbcTemplate jdbcTemplate;
	private final PasswordEncoder passwordEncoder;
	private final TenantService tenantService;

	@Override
	public void run(String... args) {
		TenantContext.setTenant("public");
		log.info("Iniciando carga de datos de prueba...");

		crearSuperAdmin();
		crearConjuntoPrueba();

		log.info("Datos de prueba cargados correctamente");
		log.info("Ver datos en: http://localhost:8080/h2-console");
		imprimirCredenciales();
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

	private void crearConjuntoPrueba() {
		if (tenantRepository.existsBySchemaName("conjunto_101")) {
			log.info("Conjunto de prueba ya existe, omitiendo...");
			return;
		}

		Tenant tenant = new Tenant();
		tenant.setSchemaName("conjunto_101");
		tenant.setNombre("Conjunto El Prado");
		tenant.setCodigo("EL-PRADO-01");
		tenant.setActivo(true);
		tenantRepository.save(tenant);
		log.info("Tenant creado: Conjunto El Prado (código: EL-PRADO-01)");

		jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS conjunto_101");
		tenantService.crearTablasTenant("conjunto_101");
		log.info("Schema conjunto_101 y tablas creados");

		crearTiposPropiedad();

		crearUsuarioConjunto("tenantadmin@prado.com", "admin123", "TENANT_ADMIN", "Carlos Admin", null, null, "ACTIVO");
		crearUsuarioConjunto("residente@prado.com", "res123", "RESIDENTE", "Juan Residente", "101", "A", "ACTIVO");
		crearUsuarioConjunto("vigilante@prado.com", "vig123", "VIGILANTE", "Luis Vigilante", null, null, "ACTIVO");
		crearUsuarioConjunto("pendiente@prado.com", "pen123", "RESIDENTE_PENDIENTE", "Pedro Pendiente", "202", "B", "PENDIENTE");
		crearUsuarioConjunto("portero@prado.com", "por123", "PORTERO", "Ana Portera", null, null, "ACTIVO");
		crearUsuarioConjunto("piscinero@prado.com", "pis123", "PISCINERO", "Mario Piscina", null, null, "ACTIVO");
		crearUsuarioConjunto("contador@prado.com", "con123", "CONTADOR", "Sofia Contadora", null, null, "ACTIVO");
	}

	private void crearTiposPropiedad() {
		Long existeCheck = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM conjunto_101.tipos_propiedad", Long.class);
		if (existeCheck != null && existeCheck > 0) {
			log.info("Tipos de propiedad ya existen, omitiendo...");
			return;
		}

		Long idTorre = jdbcTemplate.queryForObject(
				"INSERT INTO conjunto_101.tipos_propiedad (nombre, descripcion, parent_id, orden) VALUES (?, ?, NULL, 0) RETURNING id",
				Long.class, "Torre", "Bloque de apartamentos");
		jdbcTemplate.update(
				"INSERT INTO conjunto_101.tipos_propiedad (nombre, descripcion, parent_id, orden) VALUES (?, ?, ?, 0)",
				"Apto", "Unidad de vivienda", idTorre);

		jdbcTemplate.update(
				"INSERT INTO conjunto_101.tipos_propiedad (nombre, descripcion, parent_id, orden) VALUES (?, ?, NULL, 1)",
				"Parqueadero", "Espacio de parqueo");

		log.info("Tipos de propiedad de prueba creados (Torre->Apto, Parqueadero)");
	}

	private void crearUsuarioConjunto(String email, String password, String rol, String nombre, String apto,
			String torre, String estado) {
		Identidad identidad = new Identidad();
		identidad.setEmail(email);
		identidad.setPassword(passwordEncoder.encode(password));
		identidad.setRol(rol);
		identidad.setTenantId("conjunto_101");
		identidadRepository.save(identidad);

		jdbcTemplate.update("""
				INSERT INTO conjunto_101.usuarios (nombre, identidad_id, apto, torre, estado)
				VALUES (?, ?, ?, ?, ?)
				""", nombre, identidad.getId(), apto, torre, estado);

		log.info("{} creado: {} / {}", rol, email, password);
	}

	private void imprimirCredenciales() {
		log.info("""
		    
		    CREDENCIALES DE PRUEBA
		    SUPER ADMIN:    admin@app.com / admin123
		    CONJUNTO: El Prado  (codigo: EL-PRADO-01)
		    TENANT_ADMIN:   tenantadmin@prado.com / admin123
		    RESIDENTE:      residente@prado.com   / res123
		    VIGILANTE:      vigilante@prado.com   / vig123
		    PORTERO:        portero@prado.com     / por123
		    PISCINERO:      piscinero@prado.com   / pis123
		    CONTADOR:       contador@prado.com    / con123
		    PEND.APROB.:    pendiente@prado.com   / pen123
		    """);
	}

}
