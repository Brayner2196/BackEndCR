package com.backendcr.residentialcomplex.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
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
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(String... args) {
		TenantContext.setTenant("public");
		log.info("Iniciando carga de datos de prueba...");

		// Las tablas del schema public las crea PublicSchemaInitializer (Order 0),
		// que corre en todos los perfiles. Aquí solo se siembran datos de prueba.
		crearSuperAdmin();

		log.info("Datos de prueba cargados correctamente");
		imprimirCredenciales();
	}

	// ─── Datos de prueba ──────────────────────────────────────────────────────

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
