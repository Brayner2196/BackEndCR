package com.backendcr.residentialcomplex.tenant.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.dto.propiedad.TipoPropiedadNodoDto;
import com.backendcr.residentialcomplex.entity.Identidad;
import com.backendcr.residentialcomplex.entity.Tenant;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.service.pasarela.PasarelaOrchestrator;
import com.backendcr.residentialcomplex.tenant.dto.ActualizarTenantRequest;
import com.backendcr.residentialcomplex.tenant.dto.CrearTenantRequest;
import com.backendcr.residentialcomplex.tenant.dto.CrearTenantResponse;
import com.backendcr.residentialcomplex.tenant.dto.TenantResponse;
import com.backendcr.residentialcomplex.tenant.repository.TenantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final IdentidadRepository identidadRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final PasarelaOrchestrator pasarelaOrchestrator;

    @Transactional
    public CrearTenantResponse crearTenant(CrearTenantRequest request) {

        if (tenantRepository.existsBySchemaName(request.schemaName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un tenant con ese schemaName");
        }

        if (identidadRepository.existsByEmailAndTenantId(request.emailAdmin(), request.schemaName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una identidad con ese email para este tenant");
        }

        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + request.schemaName());

        crearTablasTenant(request.schemaName());

        Identidad identidad = new Identidad();
        identidad.setEmail(request.emailAdmin());
        identidad.setPassword(passwordEncoder.encode(request.passwordAdmin()));
        identidad.setRol("TENANT_ADMIN");
        identidad.setTenantId(request.schemaName());
        identidad = identidadRepository.save(identidad);

        jdbcTemplate.update("""
                INSERT INTO %s.usuarios (nombre, identidad_id, estado)
                VALUES (?, ?, 'ACTIVO')
                """.formatted(request.schemaName()),
                "Administrador", identidad.getId());

        if (request.tiposPropiedad() != null && !request.tiposPropiedad().isEmpty()) {
            insertarTiposPropiedad(request.schemaName(), request.tiposPropiedad(), null, 0);
        }

        Tenant tenant = new Tenant();
        tenant.setSchemaName(request.schemaName());
        tenant.setNombre(request.nombre());
        tenant.setCodigo(request.codigo());
        tenant.setDireccion(request.direccion());
        tenant.setTimezone(request.timezone() != null && !request.timezone().isBlank()
                ? request.timezone() : "America/Bogota");
        tenant.setActivo(true);
        tenant = tenantRepository.save(tenant);

        // Configurar pasarelas de pago si se enviaron en la creación
        if (request.pasarelas() != null && !request.pasarelas().isEmpty()) {
            final Long tenantId = tenant.getId();
            request.pasarelas().forEach(pasarelaReq -> {
                try {
                    pasarelaOrchestrator.crearOActualizarPasarela(tenantId, pasarelaReq);
                    log.info("Pasarela {} configurada para tenant '{}'",
                            pasarelaReq.tipoPasarela(), request.schemaName());
                } catch (Exception e) {
                    log.warn("Error configurando pasarela {} para tenant '{}': {}",
                            pasarelaReq.tipoPasarela(), request.schemaName(), e.getMessage());
                }
            });
        }

        return new CrearTenantResponse(
                tenant.getId(),
                tenant.getSchemaName(),
                tenant.getNombre(),
                tenant.getCodigo(),
                tenant.getTimezone(),
                new CrearTenantResponse.AdminInfo(identidad.getId(), identidad.getEmail())
        );
    }

    public List<TenantResponse> obtenerTenants() {
        return tenantRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public TenantResponse obtenerPorId(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tenant no encontrado con id: " + id));
        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse actualizarTenant(Long id, ActualizarTenantRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tenant no encontrado con id: " + id));

        if (!tenant.getCodigo().equals(request.codigo())
                && tenantRepository.existsByCodigoAndIdNot(request.codigo(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un tenant con ese código");
        }

        tenant.setNombre(request.nombre());
        tenant.setCodigo(request.codigo());
        tenant.setDireccion(request.direccion());
        if (request.activo() != null) {
            tenant.setActivo(request.activo());
        }
        if (request.timezone() != null && !request.timezone().isBlank()) {
            tenant.setTimezone(request.timezone());
        }

        return toResponse(tenantRepository.save(tenant));
    }

    @Transactional
    public void desactivarTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tenant no encontrado con id: " + id));
        tenant.setActivo(false);
        tenantRepository.save(tenant);
    }

    @Transactional
    public void activarTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tenant no encontrado con id: " + id));
        tenant.setActivo(true);
        tenantRepository.save(tenant);
    }

    @SuppressWarnings("null")
	private TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getSchemaName(),
                tenant.getNombre(),
                tenant.getCodigo(),
                tenant.isActivo(),
                tenant.getDireccion(),
                tenant.getTimezone(),
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + tenant.getSchemaName() + ".usuarios", Integer.class)
        );
    }

    /**
     * Crea todas las tablas del schema de un tenant en el orden correcto
     * respetando las dependencias de foreign keys.
     *
     * Orden:
     *  1. usuarios
     *  2. tipos_propiedad
     *  3. propiedades
     *  4. usuario_propiedades
     *  5. pqrs
     *  6. pqr_historial           ← trazabilidad de estados PQR
     *  7. anuncios
     *  8. anuncio_vistas
     *  9. zonas_comunes
     * 10. horarios_grupos_zona
     * 11. franjas_horarias
     * 12. excepciones_zonas_comunes
     * 13. reservas
     * 1. periodos_cobro
     * 1. configuracion_cuotas    ← +tipo_propiedad_condicion_id, +fecha_vigencia_hasta
     * 1. configuracion_mora
     * 1. cobros                  ← periodo_id nullable (cobros especiales)
     * 1. pagos
     * 1. abonos
     * 1. movimientos_abono
     * 1. saldos_favor
     * 1. votaciones
     * 2. opciones_votacion
     * 2. votos_residentes
     * 2. inquilino_permisos
     * 2. publicaciones
     * 2. solicitudes
     * 2. configuracion_plan_pago
     * 2. planes_pago
     * 2. cuotas_plan
     * 2. presupuestos
     * 2. categorias_presupuesto
     * 2. gastos_registrados
     * 2. configuracion_parqueadero
     * 2. parqueaderos
     * 2. vehiculos
     */
    public void crearTablasTenant(String schema) {

        // ── 1. usuarios ───────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.usuarios (
                    id             BIGSERIAL PRIMARY KEY,
                    nombre         VARCHAR(100) NOT NULL,
                    identidad_id   BIGINT NOT NULL,
                    telefono       VARCHAR(20),
                    estado         VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
                    creado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema));
        log.info("Tabla usuarios creada para tenant '{}'", schema);

        // ── 2. tipos_propiedad ────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.tipos_propiedad (
                    id            BIGSERIAL PRIMARY KEY,
                    nombre        VARCHAR(100) NOT NULL,
                    descripcion   VARCHAR(255),
                    parent_id     BIGINT REFERENCES %s.tipos_propiedad(id),
                    orden         INT NOT NULL DEFAULT 0,
                    es_facturable BOOLEAN NOT NULL DEFAULT FALSE,
                    activo        BOOLEAN NOT NULL DEFAULT TRUE
                )
                """.formatted(schema, schema));
        log.info("Tabla tipos_propiedad creada para tenant '{}'", schema);

        // ── 3. propiedades ────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.propiedades (
                    id             BIGSERIAL PRIMARY KEY,
                    tipo_id        BIGINT NOT NULL REFERENCES %s.tipos_propiedad(id),
                    identificador  VARCHAR(50) NOT NULL,
                    parent_id      BIGINT REFERENCES %s.propiedades(id),
                    estado         VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
                    creado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema, schema));
        log.info("Tabla propiedades creada para tenant '{}'", schema);

        // ── 4. usuario_propiedades ────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.usuario_propiedades (
                    id           BIGSERIAL PRIMARY KEY,
                    usuario_id   BIGINT NOT NULL,
                    propiedad_id BIGINT NOT NULL REFERENCES %s.propiedades(id),
                    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
                    rol          VARCHAR(20),
                    creado_en    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(usuario_id, propiedad_id)
                )
                """.formatted(schema, schema));
        log.info("Tabla usuario_propiedades creada para tenant '{}'", schema);

        // ── 5. pqrs ───────────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.pqrs (
                    id               BIGSERIAL PRIMARY KEY,
                    tipo             VARCHAR(12) NOT NULL,
                    asunto           VARCHAR(200) NOT NULL,
                    descripcion      VARCHAR(500) NOT NULL,
                    estado           VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                    residente_id     BIGINT NOT NULL,
                    propiedad_id     BIGINT,
                    respuesta_admin  VARCHAR(500),
                    respondido_por   BIGINT,
                    fecha_respuesta  TIMESTAMP,
                    creado_en        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema));
        log.info("Tabla pqrs creada para tenant '{}'", schema);
        

        // ── 6. pqr_historial ─────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.pqr_historial (
                    id              BIGSERIAL PRIMARY KEY,
                    pqr_id          BIGINT NOT NULL REFERENCES %s.pqrs(id),
                    estado_anterior VARCHAR(20),
                    estado_nuevo    VARCHAR(20) NOT NULL,
                    cambiado_por    BIGINT,
                    comentario      VARCHAR(500),
                    fecha_cambio    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema));
        log.info("Tabla pqr_historial creada para tenant '{}'", schema);

        // ── 7. anuncios ───────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.anuncios (
                    id             BIGSERIAL PRIMARY KEY,
                    titulo         VARCHAR(200) NOT NULL,
                    contenido      VARCHAR(2500) NOT NULL,
                    estado         VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
                    creado_por     BIGINT NOT NULL REFERENCES %s.usuarios(id),
                    fecha_inicio   TIMESTAMP,
                    fecha_fin      TIMESTAMP,
                    creado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema));
        log.info("Tabla anuncios creada para tenant '{}'", schema);

        // ── 8. anuncio_vistas ─────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.anuncio_vistas (
                    id               BIGSERIAL PRIMARY KEY,
                    anuncio_id       BIGINT NOT NULL REFERENCES %s.anuncios(id),
                    residente_id     BIGINT NOT NULL REFERENCES %s.usuarios(id),
                    residente_nombre VARCHAR(150),
                    visto_en         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(anuncio_id, residente_id)
                )
                """.formatted(schema, schema, schema));
        log.info("Tabla anuncio_vistas creada para tenant '{}'", schema);

        // ── 9. zonas_comunes ─────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.zonas_comunes (
                    id                       BIGSERIAL    PRIMARY KEY,
                    nombre                   VARCHAR(100) NOT NULL,
                    descripcion              VARCHAR(500),
                    categoria                VARCHAR(20)  NOT NULL DEFAULT 'OTRO',
                    capacidad                INT          NOT NULL DEFAULT 0,
                    activa                   BOOLEAN      NOT NULL DEFAULT TRUE,

                    -- Modo de uso
                    uso_exclusivo            BOOLEAN      NOT NULL DEFAULT TRUE,
                    buffer_limpieza_minutos  INT          NOT NULL DEFAULT 0,

                    -- Horario legacy
                    hora_apertura            TIME,
                    hora_cierre              TIME,
                    dias_disponibles         VARCHAR(100),

                    -- Reglas de duración
                    duracion_min_minutos     INT,
                    duracion_max_minutos     INT,

                    -- Reglas de anticipación
                    anticipacion_min_dias    INT,
                    anticipacion_max_dias    INT,

                    -- Cuota por residente
                    max_reservas_semana      INT,
                    max_reservas_mes         INT,
                    cancelacion_horas_antes  INT,

                    -- Aprobación
                    requiere_aprobacion      BOOLEAN      NOT NULL DEFAULT TRUE,
                    modo_aprobacion          VARCHAR(15)  NOT NULL DEFAULT 'MANUAL',

                    -- Costo
                    tiene_costo              BOOLEAN      NOT NULL DEFAULT FALSE,
                    modo_tarifa              VARCHAR(15)  NOT NULL DEFAULT 'FIJA',
                    tarifa_monto             NUMERIC(14,2),
                    deposito_monto           NUMERIC(14,2),

                    -- Restricciones
                    solo_propietarios        BOOLEAN      NOT NULL DEFAULT FALSE,
                    sin_deuda_pendiente      BOOLEAN      NOT NULL DEFAULT FALSE,
                    edad_minima              INT,
                    solo_torre               VARCHAR(50),

                    -- Suspensión
                    suspendida               BOOLEAN      NOT NULL DEFAULT FALSE,
                    motivo_suspension        VARCHAR(300),

                    creado_en                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema));
        log.info("Tabla zonas_comunes creada para tenant '{}'", schema);

        // ── 10. horarios_grupos_zona ──────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.horarios_grupos_zona (
                    id            BIGSERIAL    PRIMARY KEY,
                    zona_comun_id BIGINT       NOT NULL REFERENCES %s.zonas_comunes(id) ON DELETE CASCADE,
                    etiqueta      VARCHAR(80)  NOT NULL,
                    dias          VARCHAR(100) NOT NULL,
                    nota          VARCHAR(200),
                    orden         INT          NOT NULL DEFAULT 0
                )
                """.formatted(schema, schema));
        log.info("Tabla horarios_grupos_zona creada para tenant '{}'", schema);

        // ── 11. franjas_horarias ──────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.franjas_horarias (
                    id          BIGSERIAL PRIMARY KEY,
                    grupo_id    BIGINT    NOT NULL REFERENCES %s.horarios_grupos_zona(id) ON DELETE CASCADE,
                    hora_inicio TIME      NOT NULL,
                    hora_fin    TIME      NOT NULL,
                    orden       INT       NOT NULL DEFAULT 0
                )
                """.formatted(schema, schema));
        log.info("Tabla franjas_horarias creada para tenant '{}'", schema);

        // ── 12. excepciones_zonas_comunes ─────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.excepciones_zonas_comunes (
                    id            BIGSERIAL   PRIMARY KEY,
                    zona_comun_id BIGINT      NOT NULL REFERENCES %s.zonas_comunes(id) ON DELETE CASCADE,
                    fecha         DATE        NOT NULL,
                    tipo          VARCHAR(30) NOT NULL,
                    hora_apertura TIME,
                    hora_cierre   TIME,
                    motivo        VARCHAR(300),
                    creado_en     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema));
        log.info("Tabla excepciones_zonas_comunes creada para tenant '{}'", schema);
        	
        // ── 11. reservas ──────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.reservas (
                    id               BIGSERIAL PRIMARY KEY,
                    zona_comun_id    BIGINT NOT NULL REFERENCES %s.zonas_comunes(id),
                    residente_id     BIGINT NOT NULL REFERENCES %s.usuarios(id),
                    propiedad_id     BIGINT NOT NULL REFERENCES %s.propiedades(id),
                    fecha            DATE NOT NULL,
                    hora_inicio      TIME NOT NULL,
                    hora_fin         TIME NOT NULL,
                    estado           VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                    observaciones    VARCHAR(500),
                    decidido_por     BIGINT REFERENCES %s.usuarios(id),
                    motivo_decision  VARCHAR(300),
                    fecha_decision   TIMESTAMP,
                    creado_en        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema, schema, schema, schema));
        log.info("Tabla reservas creada para tenant '{}'", schema);

        // ── 11. periodos_cobro ────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.periodos_cobro (
                    id                  BIGSERIAL PRIMARY KEY,
                    anio                INT NOT NULL,
                    mes                 INT NOT NULL,
                    fecha_inicio        DATE NOT NULL,
                    fecha_fin           DATE NOT NULL,
                    fecha_limite_pago   DATE NOT NULL,
                    estado              VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
                    creado_por          BIGINT NOT NULL REFERENCES %s.usuarios(id),
                    creado_en           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(anio, mes)
                )
                """.formatted(schema, schema));
        log.info("Tabla periodos_cobro creada para tenant '{}'", schema);

        // ── 12. configuracion_cuotas ──────────────────────────────────────
        // tipo_propiedad_condicion_id: tipo ancestro sobre el que se evalúa
        //   el rango numérico (null = usa el identificador propio de la propiedad).
        // fecha_vigencia_hasta: null = sin fecha de fin (vigente indefinidamente).
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.configuracion_cuotas (
                    id                          BIGSERIAL PRIMARY KEY,
                    tipo_propiedad_id           BIGINT REFERENCES %s.tipos_propiedad(id),
                    propiedad_id                BIGINT REFERENCES %s.propiedades(id),
                    tipo_propiedad_condicion_id BIGINT REFERENCES %s.tipos_propiedad(id),
                    numero_desde                INT,
                    numero_hasta                INT,
                    monto                       NUMERIC(12,0) NOT NULL,
                    periodicidad                VARCHAR(20) NOT NULL DEFAULT 'MENSUAL',
                    fecha_vigencia_desde        DATE NOT NULL,
                    fecha_vigencia_hasta        DATE,
                    activo                      BOOLEAN NOT NULL DEFAULT TRUE,
                    creado_en                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema, schema, schema));
        log.info("Tabla configuracion_cuotas creada para tenant '{}'", schema);

        // ── 13. configuracion_mora ────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.configuracion_mora (
                    id                  BIGSERIAL PRIMARY KEY,
                    porcentaje_mensual  NUMERIC(5,2),
                    dias_gracia         INT NOT NULL DEFAULT 0,
                    tipo_calculo        VARCHAR(20) NOT NULL DEFAULT 'PORCENTAJE',
                    monto_fijo          NUMERIC(12,0),
                    activo              BOOLEAN NOT NULL DEFAULT TRUE,
                    fecha_vigencia      DATE NOT NULL,
                    creado_en           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema));
        log.info("Tabla configuracion_mora creada para tenant '{}'", schema);

        // ── 14. cobros ────────────────────────────────────────────────────
        // periodo_id es nullable: null = cobro especial (multa, sanción, etc.)
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.cobros (
                    id                   BIGSERIAL PRIMARY KEY,
                    periodo_id           BIGINT REFERENCES %s.periodos_cobro(id),
                    propiedad_id         BIGINT NOT NULL REFERENCES %s.propiedades(id),
                    usuario_id           BIGINT,
                    concepto             VARCHAR(20) NOT NULL DEFAULT 'ADMINISTRACION',
                    descripcion          VARCHAR(200),
                    monto_base           NUMERIC(12,0) NOT NULL,
                    monto_mora           NUMERIC(12,0) NOT NULL DEFAULT 0,
                    monto_total          NUMERIC(12,0),
                    monto_pagado         NUMERIC(12,0) NOT NULL DEFAULT 0,
                    fecha_generacion     DATE NOT NULL,
                    fecha_limite_pago    DATE NOT NULL,
                    estado               VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                    exonerado_por        BIGINT,
                    nota_exoneracion     VARCHAR(300),
                    creado_en            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema, schema));
        log.info("Tabla cobros creada para tenant '{}'", schema);

        // ── 15. pagos ─────────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.pagos (
                    id                  BIGSERIAL PRIMARY KEY,
                    cobro_id            BIGINT NOT NULL REFERENCES %s.cobros(id),
                    usuario_id          BIGINT NOT NULL REFERENCES %s.usuarios(id),
                    monto_pagado        NUMERIC(12,0) NOT NULL,
                    fecha_pago          DATE NOT NULL,
                    metodo_pago         VARCHAR(20) NOT NULL,
                    referencia          VARCHAR(100),
                    url_comprobante     VARCHAR(500),
                    estado              VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_VERIFICACION',
                    verificado_por      BIGINT REFERENCES %s.usuarios(id),
                    fecha_verificacion  TIMESTAMP,
                    motivo_rechazo      VARCHAR(300),
                    creado_en           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema, schema, schema));
        	log.info("Tabla pagos creada para tenant '{}'", schema);

        // ── 16. abonos ────────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.abonos (
                    id                  BIGSERIAL PRIMARY KEY,
                    propiedad_id        BIGINT NOT NULL REFERENCES %s.propiedades(id),
                    usuario_id          BIGINT NOT NULL REFERENCES %s.usuarios(id),
                    monto_total         NUMERIC(12,0) NOT NULL,
                    fecha_pago          DATE NOT NULL,
                    metodo_pago         VARCHAR(20) NOT NULL,
                    referencia          VARCHAR(100),
                    url_comprobante     VARCHAR(500),
                    notas               VARCHAR(500),
                    estado              VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_VERIFICACION',
                    verificado_por      BIGINT,
                    fecha_verificacion  TIMESTAMP,
                    motivo_rechazo      VARCHAR(300),
                    creado_en           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema, schema));
        log.info("Tabla abonos creada para tenant '{}'", schema);

        // ── 17. movimientos_abono ─────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.movimientos_abono (
                    id              BIGSERIAL PRIMARY KEY,
                    abono_id        BIGINT REFERENCES %s.abonos(id),
                    cobro_id        BIGINT REFERENCES %s.cobros(id),
                    monto_aplicado  NUMERIC(12,0) NOT NULL,
                    descripcion     VARCHAR(100),
                    creado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema, schema));
        	log.info("Tabla movimientos_abono creada para tenant '{}'", schema);

        // ── 18. saldos_favor ──────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.saldos_favor (
                    id              BIGSERIAL PRIMARY KEY,
                    propiedad_id    BIGINT NOT NULL UNIQUE REFERENCES %s.propiedades(id),
                    usuario_id      BIGINT NOT NULL,
                    saldo           NUMERIC(12,0) NOT NULL DEFAULT 0,
                    creado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema));
        	log.info("Tabla saldos_favor creada para tenant '{}'", schema);

        // ── 19. votaciones ────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.votaciones (
                    id                   BIGSERIAL PRIMARY KEY,
                    titulo               VARCHAR(300) NOT NULL,
                    descripcion          VARCHAR(1000),
                    tipo_votacion        VARCHAR(30) NOT NULL,
                    estado               VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
                    escala_max           INT,
                    mostrar_votantes     BOOLEAN NOT NULL DEFAULT FALSE,
                    permite_cambiar_voto BOOLEAN NOT NULL DEFAULT FALSE,
                    mostrar_porcentajes BOOLEAN NOT NULL DEFAULT FALSE,
                    fecha_inicio         TIMESTAMP,
                    fecha_fin            TIMESTAMP,
                    creado_por           BIGINT NOT NULL REFERENCES %s.usuarios(id),
                    creado_en            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema));
        	log.info("Tabla votaciones creada para tenant '{}'", schema);

        // ── 20. opciones_votacion ─────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.opciones_votacion (
                    id          BIGSERIAL PRIMARY KEY,
                    votacion_id BIGINT NOT NULL REFERENCES %s.votaciones(id),
                    texto       VARCHAR(300) NOT NULL,
                    orden       INT NOT NULL DEFAULT 0
                )
                """.formatted(schema, schema));
        	log.info("Tabla opciones_votacion creada para tenant '{}'", schema);

        // ── 21. votos_residentes ──────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.votos_residentes (
                    id               BIGSERIAL PRIMARY KEY,
                    votacion_id      BIGINT NOT NULL REFERENCES %s.votaciones(id),
                    residente_id     BIGINT NOT NULL REFERENCES %s.usuarios(id),
                    residente_nombre VARCHAR(150),
                    opcion_id        BIGINT REFERENCES %s.opciones_votacion(id),
                    valor_numerico   INT,
                    respuesta_texto  VARCHAR(1000),
                    votado_en        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema, schema,schema));
        	log.info("Tabla votos_residentes creada para tenant '{}'", schema);
        	
    	// ── 21. inquilino_permisos ──────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.inquilino_permisos (
                    id               BIGSERIAL PRIMARY KEY,
                    inquilino_id      BIGINT NOT NULL REFERENCES %s.usuarios(id),
                    propietario_id    BIGINT NOT NULL REFERENCES %s.usuarios(id),
                    permiso        VARCHAR(30) NOT NULL
                )
                """.formatted(schema, schema, schema));
        	log.info("Tabla inquilino_permisos creada para tenant '{}'", schema);
        	
    	// ── 21. publicaciones ──────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.publicaciones (
                    id               BIGSERIAL PRIMARY KEY,
                    vendedor_id      BIGINT NOT NULL REFERENCES %s.usuarios(id),
                    vendedor_nombre	 VARCHAR(150) NOT NULL,
                    propiedad_id     BIGINT NOT NULL REFERENCES %s.propiedades(id),
                    titulo           VARCHAR(120) NOT NULL,
                    descripcion      VARCHAR(1000),
                    precio           NUMERIC(12,0) NOT NULL,
                    categoria		 VARCHAR(30) NOT NULL,
                    contacto		 VARCHAR(100),
                    marca			 VARCHAR(50),
                    stock			 INT,
                    acepta_domicilio BOOLEAN NOT NULL DEFAULT FALSE,
                    metodos_pago	 VARCHAR(300),
                    estado		     VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
                    creado_en        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema, schema));
        	log.info("Tabla publicaciones creada para tenant '{}'", schema);
        	
        	
        	// ── 21. solicitudes ──────────────────────────────────────────
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS %s.solicitudes (
                        id                 BIGSERIAL PRIMARY KEY,
                        publicacion_id     BIGINT NOT NULL REFERENCES %s.publicaciones(id),
                        publicacion_titulo VARCHAR(120) NOT NULL,
                        publicacion_precio NUMERIC(12,0) NOT NULL,
                        comprador_id       BIGINT NOT NULL REFERENCES %s.usuarios(id),
                        comprador_nombre   VARCHAR(150) NOT NULL,
                        vendedor_id        BIGINT NOT NULL REFERENCES %s.usuarios(id),
                        vendedor_nombre	   VARCHAR(150) NOT NULL,
                        tipo               VARCHAR(20) NOT NULL,
                        cantidad		   INT NOT NULL,
                        notas			   VARCHAR(300),
                        estado             VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                        creado_en        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        actualizado_en   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """.formatted(schema, schema, schema, schema));
            	log.info("Tabla solicitudes creada para tenant '{}'", schema);
            	
            	jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS %s.configuracion_plan_pago (
                            id                          BIGSERIAL PRIMARY KEY,
                            activo                      BOOLEAN NOT NULL DEFAULT FALSE,
                            max_cuotas                  INT NOT NULL DEFAULT 3,
                            recargo_fraccionamiento     BOOLEAN NOT NULL DEFAULT FALSE,
                            porcentaje_recargo          NUMERIC(5,2) NOT NULL DEFAULT 0,
                            mora_congelada_durante_plan BOOLEAN NOT NULL DEFAULT FALSE,
                            aprobacion_automatica       BOOLEAN NOT NULL DEFAULT FALSE,
                            actualizado_en              TIMESTAMP
                        )
                        """.formatted(schema));
                log.info("Tabla configuracion_plan_pago creada/verificada para tenant '{}'", schema);

                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS %s.planes_pago (
                            id                  BIGSERIAL PRIMARY KEY,
                            propiedad_id        BIGINT NOT NULL,
                            residente_id        BIGINT NOT NULL,
                            monto_total_deuda   NUMERIC(12,0) NOT NULL,
                            numero_cuotas       INT NOT NULL,
                            monto_recargo       NUMERIC(12,0) NOT NULL DEFAULT 0,
                            monto_total_plan    NUMERIC(12,0) NOT NULL,
                            estado              VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                            cobros_incluidos    VARCHAR(500),
                            observaciones       VARCHAR(500),
                            motivo_rechazo      VARCHAR(300),
                            nota_admin          VARCHAR(300),
                            aprobado_por        BIGINT,
                            fecha_decision      TIMESTAMP,
                            creado_en           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            actualizado_en      TIMESTAMP
                        )
                        """.formatted(schema));
                log.info("Tabla planes_pago creada/verificada para tenant '{}'", schema);

                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS %s.cuotas_plan (
                            id               BIGSERIAL PRIMARY KEY,
                            plan_id          BIGINT NOT NULL REFERENCES %s.planes_pago(id),
                            numero_cuota     INT NOT NULL,
                            monto            NUMERIC(12,0) NOT NULL,
                            fecha_vencimiento DATE NOT NULL,
                            estado           VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                            fecha_pago       DATE,
                            nota_pago        VARCHAR(300),
                            actualizado_en   TIMESTAMP
                        )
                        """.formatted(schema, schema));
                log.info("Tabla cuotas_plan creada/verificada para tenant '{}'", schema);
                
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS %s.presupuestos (
                            id                          BIGSERIAL PRIMARY KEY,
                            anio                        INT NOT NULL,
                            titulo                      VARCHAR(150),
                            monto_total_presupuestado   NUMERIC(15,0) NOT NULL DEFAULT 0,
                            activo                      BOOLEAN NOT NULL DEFAULT FALSE,
                            creado_en                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            actualizado_en              TIMESTAMP
                        )
                        """.formatted(schema));
                log.info("Tabla presupuestos creada/verificada para tenant '{}'", schema);

                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS %s.categorias_presupuesto (
                            id              BIGSERIAL PRIMARY KEY,
                            presupuesto_id  BIGINT NOT NULL REFERENCES %s.presupuestos(id),
                            nombre          VARCHAR(100) NOT NULL,
                            descripcion     VARCHAR(300),
                            monto_asignado  NUMERIC(15,0) NOT NULL,
                            color           VARCHAR(10),
                            icono           VARCHAR(80),
                            creado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            actualizado_en  TIMESTAMP
                        )
                        """.formatted(schema, schema));
                log.info("Tabla categorias_presupuesto creada/verificada para tenant '{}'", schema);

                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS %s.gastos_registrados (
                            id              BIGSERIAL PRIMARY KEY,
                            presupuesto_id  BIGINT NOT NULL REFERENCES %s.presupuestos(id),
                            categoria_id    BIGINT NOT NULL REFERENCES %s.categorias_presupuesto(id),
                            descripcion     VARCHAR(300) NOT NULL,
                            monto           NUMERIC(15,0) NOT NULL,
                            fecha           DATE NOT NULL,
                            comprobante     VARCHAR(500),
                            registrado_por  BIGINT NOT NULL,
                            creado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """.formatted(schema, schema, schema));
                log.info("Tabla gastos_registrados creada/verificada para tenant '{}'", schema);

        // ── 29. configuracion_parqueadero ─────────────────────────────────
        // Tabla singleton (una fila por tenant) con los parámetros globales
        // de parqueaderos configurados por el TENANT_ADMIN.
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.configuracion_parqueadero (
                    id                           BIGSERIAL PRIMARY KEY,
                    total_parqueaderos           INT     NOT NULL DEFAULT 0,
                    parqueaderos_comunes         INT     NOT NULL DEFAULT 0,
                    parqueaderos_privados        INT     NOT NULL DEFAULT 0,
                    max_vehiculos_por_propiedad  INT     NOT NULL DEFAULT 2,
                    permite_carro                BOOLEAN NOT NULL DEFAULT TRUE,
                    permite_moto                 BOOLEAN NOT NULL DEFAULT TRUE,
                    permite_bicicleta            BOOLEAN NOT NULL DEFAULT TRUE,
                    requiere_aprobacion_vehiculo BOOLEAN NOT NULL DEFAULT FALSE,
                    actualizado_en               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema));
        log.info("Tabla configuracion_parqueadero creada para tenant '{}'", schema);

        // ── 30. parqueaderos ──────────────────────────────────────────────
        // vehiculo_id es solo un campo de conveniencia (sin FK) para evitar
        // la dependencia circular con la tabla vehiculos.
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.parqueaderos (
                    id            BIGSERIAL    PRIMARY KEY,
                    identificador VARCHAR(30)  NOT NULL UNIQUE,
                    tipo          VARCHAR(10)  NOT NULL,
                    propiedad_id  BIGINT       REFERENCES %s.propiedades(id),
                    vehiculo_id   BIGINT,
                    creado_en     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema));
        log.info("Tabla parqueaderos creada para tenant '{}'", schema);

        // ── 31. vehiculos ─────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.vehiculos (
                    id             BIGSERIAL    PRIMARY KEY,
                    placa          VARCHAR(15)  NOT NULL,
                    tipo           VARCHAR(15)  NOT NULL,
                    marca          VARCHAR(50),
                    modelo         VARCHAR(50),
                    color          VARCHAR(30),
                    propiedad_id   BIGINT       NOT NULL REFERENCES %s.propiedades(id),
                    parqueadero_id BIGINT       REFERENCES %s.parqueaderos(id),
                    estado         VARCHAR(15)  NOT NULL DEFAULT 'PENDIENTE',
                    motivo_rechazo VARCHAR(300),
                    creado_en      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema, schema));
        log.info("Tabla vehiculos creada para tenant '{}'", schema);

    }


    private void insertarTiposPropiedad(String schema, List<TipoPropiedadNodoDto> tipos,
                                        Long parentId, int ordenBase) {
        for (int i = 0; i < tipos.size(); i++) {
            TipoPropiedadNodoDto nodo = tipos.get(i);
            Long nuevoId = jdbcTemplate.queryForObject(
                    "INSERT INTO " + schema + ".tipos_propiedad (nombre, descripcion, parent_id, orden, es_facturable) " +
                    "VALUES (?, ?, ?, ?, ?) RETURNING id",
                    Long.class,
                    nodo.nombre(), nodo.descripcion(), parentId, ordenBase + i, nodo.esFacturable());

            if (nodo.hijos() != null && !nodo.hijos().isEmpty()) {
                insertarTiposPropiedad(schema, nodo.hijos(), nuevoId, 0);
            }
        }
    }
}
