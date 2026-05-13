package com.backendcr.residentialcomplex.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
@Profile("default")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

	private static final String SCHEMA = "conjunto_101";

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
		if (tenantRepository.existsBySchemaName(SCHEMA)) {
			log.info("Conjunto de prueba ya existe, omitiendo...");
			return;
		}

		Tenant tenant = new Tenant();
		tenant.setSchemaName(SCHEMA);
		tenant.setNombre("Conjunto El Prado");
		tenant.setCodigo("EL-PRADO-01");
		tenant.setActivo(true);
		tenantRepository.save(tenant);
		log.info("Tenant creado: Conjunto El Prado (código: EL-PRADO-01)");

		jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + SCHEMA);
		tenantService.crearTablasTenant(SCHEMA);
		log.info("Schema {} y tablas creados", SCHEMA);

		crearTiposPropiedad();

		crearUsuarioConjunto("tenantadmin@prado.com", "admin123", "TENANT_ADMIN", "Carlos Admin", null, null, "ACTIVO");
		Long juanId = crearUsuarioConjunto("residente@prado.com", "res123", "RESIDENTE", "Juan Residente", "101", "A", "ACTIVO");
		crearUsuarioConjunto("vigilante@prado.com", "vig123", "VIGILANTE", "Luis Vigilante", null, null, "ACTIVO");
		crearUsuarioConjunto("pendiente@prado.com", "pen123", "RESIDENTE_PENDIENTE", "Pedro Pendiente", "202", "B", "PENDIENTE");
		crearUsuarioConjunto("portero@prado.com", "por123", "PORTERO", "Ana Portera", null, null, "ACTIVO");
		crearUsuarioConjunto("piscinero@prado.com", "pis123", "PISCINERO", "Mario Piscina", null, null, "ACTIVO");
		crearUsuarioConjunto("contador@prado.com", "con123", "CONTADOR", "Sofia Contadora", null, null, "ACTIVO");

		try {
			crearDatosDashboard(juanId);
		} catch (Exception e) {
			log.warn("No se pudieron crear los datos demo del dashboard: {}", e.getMessage(), e);
		}
	}

	private void crearTiposPropiedad() {
		Long existeCheck = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM " + SCHEMA + ".tipos_propiedad", Long.class);
		if (existeCheck != null && existeCheck > 0) {
			log.info("Tipos de propiedad ya existen, omitiendo...");
			return;
		}

		Long idTorre = jdbcTemplate.queryForObject(
				"INSERT INTO " + SCHEMA + ".tipos_propiedad (nombre, descripcion, parent_id, orden, es_facturable) VALUES (?, ?, NULL, 0, FALSE) RETURNING id",
				Long.class, "Torre", "Bloque de apartamentos");
		jdbcTemplate.update(
				"INSERT INTO " + SCHEMA + ".tipos_propiedad (nombre, descripcion, parent_id, orden, es_facturable) VALUES (?, ?, ?, 0, TRUE)",
				"Apto", "Unidad de vivienda", idTorre);

		jdbcTemplate.update(
				"INSERT INTO " + SCHEMA + ".tipos_propiedad (nombre, descripcion, parent_id, orden, es_facturable) VALUES (?, ?, NULL, 1, TRUE)",
				"Parqueadero", "Espacio de parqueo");

		log.info("Tipos de propiedad de prueba creados (Torre->Apto, Parqueadero)");
	}

	private Long crearUsuarioConjunto(String email, String password, String rol, String nombre, String apto,
			String torre, String estado) {
		Identidad identidad = new Identidad();
		identidad.setEmail(email);
		identidad.setPassword(passwordEncoder.encode(password));
		identidad.setRol(rol);
		identidad.setTenantId(SCHEMA);
		identidadRepository.save(identidad);

		Long usuarioId = jdbcTemplate.queryForObject("""
				INSERT INTO %s.usuarios (nombre, identidad_id, apto, torre, estado)
				VALUES (?, ?, ?, ?, ?) RETURNING id
				""".formatted(SCHEMA), Long.class, nombre, identidad.getId(), apto, torre, estado);

		log.info("{} creado: {} / {}", rol, email, password);
		return usuarioId;
	}

	private void crearDatosDashboard(Long residenteId) {
		log.info("Sembrando datos demo del dashboard...");

		Long idTipoTorre = jdbcTemplate.queryForObject(
				"SELECT id FROM " + SCHEMA + ".tipos_propiedad WHERE nombre = 'Torre' AND parent_id IS NULL",
				Long.class);
		Long idTipoApto = jdbcTemplate.queryForObject(
				"SELECT id FROM " + SCHEMA + ".tipos_propiedad WHERE nombre = 'Apto'",
				Long.class);

		String[] letras = { "A", "B", "C", "D" };
		Long[] torreIds = new Long[letras.length];
		for (int i = 0; i < letras.length; i++) {
			torreIds[i] = jdbcTemplate.queryForObject(
					"INSERT INTO " + SCHEMA + ".propiedades (tipo_id, identificador, parent_id, estado) VALUES (?, ?, NULL, 'OCUPADO') RETURNING id",
					Long.class, idTipoTorre, letras[i]);
		}

		List<Long> aptoIds = new ArrayList<>();
		for (int t = 0; t < letras.length; t++) {
			for (int n = 1; n <= 30; n++) {
				int piso = (n - 1) / 5 + 1;
				int num = (n - 1) % 5 + 1;
				String identificador = (piso * 100 + num) + letras[t];
				Long aptoId = jdbcTemplate.queryForObject(
						"INSERT INTO " + SCHEMA + ".propiedades (tipo_id, identificador, parent_id, estado) VALUES (?, ?, ?, 'OCUPADO') RETURNING id",
						Long.class, idTipoApto, identificador, torreIds[t]);
				aptoIds.add(aptoId);
			}
		}
		log.info("Creadas {} torres y {} apartamentos", letras.length, aptoIds.size());

		Long juanAptoId = aptoIds.get(0);
		jdbcTemplate.update(
				"INSERT INTO " + SCHEMA + ".usuario_propiedades (usuario_id, propiedad_id, es_principal) VALUES (?, ?, TRUE)",
				residenteId, juanAptoId);

		BigDecimal montoAdmin = new BigDecimal("350000");
		jdbcTemplate.update(
				"INSERT INTO " + SCHEMA + ".configuracion_cuotas (tipo_propiedad_id, monto, periodicidad, fecha_vigencia_desde, fecha_vigencia_hasta, activo) VALUES (?, ?, 'MENSUAL', ?, NULL, TRUE)",
				idTipoApto, montoAdmin, LocalDate.now().minusYears(1));

		Map<YearMonth, Long> periodos = new HashMap<>();
		LocalDate hoy = LocalDate.now();
		for (int i = 5; i >= 0; i--) {
			YearMonth ym = YearMonth.from(hoy).minusMonths(i);
			LocalDate ini = ym.atDay(1);
			LocalDate fin = ym.atEndOfMonth();
			LocalDate limite = ym.atDay(15);
			String estado = (i == 0) ? "ABIERTO" : "CERRADO";
			Long pid = jdbcTemplate.queryForObject(
					"INSERT INTO " + SCHEMA + ".periodos_cobro (anio, mes, fecha_inicio, fecha_fin, fecha_limite_pago, estado) VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
					Long.class, ym.getYear(), ym.getMonthValue(), ini, fin, limite, estado);
			periodos.put(ym, pid);
		}

		Random rng = new Random(42);
		YearMonth ymActual = YearMonth.from(hoy);
		for (Map.Entry<YearMonth, Long> e : periodos.entrySet()) {
			YearMonth ym = e.getKey();
			Long periodoId = e.getValue();
			LocalDate fechaLimite = ym.atDay(15);
			LocalDate fechaGen = ym.atDay(1);
			boolean esActual = ym.equals(ymActual);
			boolean esMesAnterior = ym.equals(ymActual.minusMonths(1));
			boolean esAntiguo = ym.isBefore(ymActual.minusMonths(1));

			for (Long aptoId : aptoIds) {
				String estado;
				if (esAntiguo) {
					estado = rng.nextDouble() < 0.95 ? "PAGADO" : "VENCIDO";
				} else if (esMesAnterior) {
					estado = rng.nextDouble() < 0.88 ? "PAGADO" : "VENCIDO";
				} else if (esActual) {
					double r = rng.nextDouble();
					if (r < 0.74) estado = "PAGADO";
					else if (r < 0.88) estado = "VENCIDO";
					else estado = "PENDIENTE";
				} else {
					estado = "PENDIENTE";
				}
				Long usuarioRefId = aptoId.equals(juanAptoId) ? residenteId : null;
				jdbcTemplate.update(
						"INSERT INTO " + SCHEMA + ".cobros (periodo_id, propiedad_id, usuario_id, concepto, monto_base, monto_mora, monto_total, fecha_generacion, fecha_limite_pago, estado) VALUES (?, ?, ?, 'ADMINISTRACION', ?, 0, ?, ?, ?, ?)",
						periodoId, aptoId, usuarioRefId, montoAdmin, montoAdmin, fechaGen, fechaLimite, estado);
			}
		}
		log.info("Cobros sembrados para {} apartamentos x 6 meses", aptoIds.size());

		Long periodoActual = periodos.get(ymActual);
		LocalDate fechaLimiteActual = ymActual.atDay(15);
		LocalDate fechaGenActual = ymActual.atDay(1);
		String[] conceptos = { "ADMINISTRACION", "ZONA_COMUN", "OTRO", "ADMINISTRACION", "MULTA", "ZONA_COMUN", "OTRO" };
		for (int i = 0; i < 7; i++) {
			BigDecimal monto = montoAdmin.add(BigDecimal.valueOf((long) (i + 1) * 1000));
			Long cobroId = jdbcTemplate.queryForObject(
					"INSERT INTO " + SCHEMA + ".cobros (periodo_id, propiedad_id, usuario_id, concepto, descripcion, monto_base, monto_mora, monto_total, fecha_generacion, fecha_limite_pago, estado) VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, ?, 'PENDIENTE') RETURNING id",
					Long.class, periodoActual, juanAptoId, residenteId, conceptos[i],
					"Pago por revisar #" + (i + 1), monto, monto, fechaGenActual, fechaLimiteActual);

			jdbcTemplate.update(
					"INSERT INTO " + SCHEMA + ".pagos (cobro_id, usuario_id, monto_pagado, fecha_pago, metodo_pago, referencia, url_comprobante, estado) VALUES (?, ?, ?, ?, 'TRANSFERENCIA', ?, ?, 'PENDIENTE_VERIFICACION')",
					cobroId, residenteId, monto, hoy.minusDays(i),
					"REF" + (1000 + i), "https://demo.local/comprobante" + i + ".jpg");
		}
		log.info("Creados 7 pagos en PENDIENTE_VERIFICACION");

		Long zonaSalon = jdbcTemplate.queryForObject(
				"INSERT INTO " + SCHEMA + ".zonas_comunes (nombre, descripcion, capacidad, activa) VALUES (?, ?, ?, TRUE) RETURNING id",
				Long.class, "Salón Social", "Salón para eventos y reuniones", 50);
		jdbcTemplate.update(
				"INSERT INTO " + SCHEMA + ".zonas_comunes (nombre, descripcion, capacidad, activa) VALUES (?, ?, ?, TRUE)",
				"Piscina", "Piscina principal del conjunto", 30);
		jdbcTemplate.update(
				"INSERT INTO " + SCHEMA + ".zonas_comunes (nombre, descripcion, capacidad, activa) VALUES (?, ?, ?, TRUE)",
				"Zona BBQ", "Asadores y mesas al aire libre", 15);

		jdbcTemplate.update(
				"INSERT INTO " + SCHEMA + ".reservas (zona_comun_id, residente_id, propiedad_id, fecha, hora_inicio, hora_fin, estado, observaciones) VALUES (?, ?, ?, ?, ?, ?, 'PENDIENTE', ?)",
				zonaSalon, residenteId, juanAptoId, hoy.plusDays(7),
				LocalTime.of(18, 0), LocalTime.of(22, 0), "Cumpleaños familiar");

		jdbcTemplate.update(
				"INSERT INTO " + SCHEMA + ".pqrs (tipo, asunto, descripcion, estado, residente_id, propiedad_id, creado_en, actualizado_en) VALUES ('QUEJA', ?, ?, 'PENDIENTE', ?, ?, ?, ?)",
				"Ruido en la noche",
				"Hay mucho ruido del apartamento del piso superior después de las 11 PM. Por favor revisar.",
				residenteId, juanAptoId, LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2));
		jdbcTemplate.update(
				"INSERT INTO " + SCHEMA + ".pqrs (tipo, asunto, descripcion, estado, residente_id, propiedad_id, creado_en, actualizado_en) VALUES ('PETICION', ?, ?, 'PENDIENTE', ?, ?, ?, ?)",
				"Solicitud de mantenimiento",
				"La iluminación del pasillo del piso 1 no funciona, requiere atención.",
				residenteId, juanAptoId, LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1));
		jdbcTemplate.update(
				"INSERT INTO " + SCHEMA + ".pqrs (tipo, asunto, descripcion, estado, residente_id, propiedad_id, creado_en, actualizado_en) VALUES ('SUGERENCIA', ?, ?, 'EN_PROCESO', ?, ?, ?, ?)",
				"Mejorar el sistema de cámaras",
				"Sugiero instalar cámaras de mayor resolución en la entrada principal.",
				residenteId, juanAptoId, LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(3));

		log.info("Datos demo del dashboard creados: 120 apartamentos, 7 pagos pendientes, 1 reserva, 3 PQRs");
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
