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
import com.backendcr.residentialcomplex.tenant.dto.ActualizarTenantRequest;
import com.backendcr.residentialcomplex.tenant.dto.CrearTenantRequest;
import com.backendcr.residentialcomplex.tenant.dto.CrearTenantResponse;
import com.backendcr.residentialcomplex.tenant.dto.TenantResponse;
import com.backendcr.residentialcomplex.tenant.repository.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final IdentidadRepository identidadRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

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
        tenant.setActivo(true);
        tenant = tenantRepository.save(tenant);

        return new CrearTenantResponse(
                tenant.getId(),
                tenant.getSchemaName(),
                tenant.getNombre(),
                tenant.getCodigo(),
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

    private TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getSchemaName(),
                tenant.getNombre(),
                tenant.getCodigo(),
                tenant.isActivo(),
                tenant.getDireccion(),
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
     * 10. reservas
     * 11. periodos_cobro
     * 12. configuracion_cuotas    ← +tipo_propiedad_condicion_id, +fecha_vigencia_hasta
     * 13. configuracion_mora
     * 14. cobros                  ← periodo_id nullable (cobros especiales)
     * 15. pagos
     * 16. abonos
     * 17. movimientos_abono
     * 18. saldos_favor
     * 19. votaciones
     * 20. opciones_votacion
     * 21. votos_residentes
     */
    public void crearTablasTenant(String schema) {

        // ── 1. usuarios ───────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.usuarios (
                    id             BIGSERIAL PRIMARY KEY,
                    nombre         VARCHAR(100) NOT NULL,
                    identidad_id   BIGINT NOT NULL,
                    apto           VARCHAR(20),
                    torre          VARCHAR(20),
                    telefono       VARCHAR(20),
                    estado         VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
                    creado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema));

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

        // ── 3. propiedades ────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.propiedades (
                    id             BIGSERIAL PRIMARY KEY,
                    tipo_id        BIGINT NOT NULL REFERENCES %s.tipos_propiedad(id),
                    identificador  VARCHAR(50) NOT NULL,
                    parent_id      BIGINT REFERENCES %s.propiedades(id),
                    estado         VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE',
                    creado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema, schema));

        // ── 4. usuario_propiedades ────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.usuario_propiedades (
                    id           BIGSERIAL PRIMARY KEY,
                    usuario_id   BIGINT NOT NULL,
                    propiedad_id BIGINT NOT NULL REFERENCES %s.propiedades(id),
                    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
                    creado_en    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(usuario_id, propiedad_id)
                )
                """.formatted(schema, schema));

        // ── 5. pqrs ───────────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.pqrs (
                    id               BIGSERIAL PRIMARY KEY,
                    tipo             VARCHAR(20) NOT NULL,
                    asunto           VARCHAR(200) NOT NULL,
                    descripcion      VARCHAR(2000) NOT NULL,
                    estado           VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                    residente_id     BIGINT NOT NULL,
                    propiedad_id     BIGINT,
                    respuesta_admin  VARCHAR(2000),
                    respondido_por   BIGINT,
                    fecha_respuesta  TIMESTAMP,
                    creado_en        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema));

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

        // ── 7. anuncios ───────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.anuncios (
                    id             BIGSERIAL PRIMARY KEY,
                    titulo         VARCHAR(200) NOT NULL,
                    contenido      VARCHAR(4000) NOT NULL,
                    imagen_url     VARCHAR(500),
                    estado         VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
                    creado_por     BIGINT NOT NULL,
                    fecha_inicio   TIMESTAMP,
                    fecha_fin      TIMESTAMP,
                    creado_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema));

        // ── 8. anuncio_vistas ─────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.anuncio_vistas (
                    id               BIGSERIAL PRIMARY KEY,
                    anuncio_id       BIGINT NOT NULL REFERENCES %s.anuncios(id),
                    residente_id     BIGINT NOT NULL,
                    residente_nombre VARCHAR(150),
                    visto_en         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(anuncio_id, residente_id)
                )
                """.formatted(schema, schema));

        // ── 9. zonas_comunes ──────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.zonas_comunes (
                    id          BIGSERIAL PRIMARY KEY,
                    nombre      VARCHAR(100) NOT NULL,
                    descripcion VARCHAR(500),
                    capacidad   INT NOT NULL DEFAULT 0,
                    activa      BOOLEAN NOT NULL DEFAULT TRUE,
                    creado_en   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema));

        // ── 10. reservas ──────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.reservas (
                    id               BIGSERIAL PRIMARY KEY,
                    zona_comun_id    BIGINT NOT NULL REFERENCES %s.zonas_comunes(id),
                    residente_id     BIGINT NOT NULL,
                    propiedad_id     BIGINT,
                    fecha            DATE NOT NULL,
                    hora_inicio      TIME NOT NULL,
                    hora_fin         TIME NOT NULL,
                    estado           VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                    observaciones    VARCHAR(500),
                    decidido_por     BIGINT,
                    motivo_decision  VARCHAR(300),
                    fecha_decision   TIMESTAMP,
                    creado_en        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema));

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
                    creado_en           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(anio, mes)
                )
                """.formatted(schema));

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
                    monto                       NUMERIC(12,2) NOT NULL,
                    periodicidad                VARCHAR(20) NOT NULL DEFAULT 'MENSUAL',
                    fecha_vigencia_desde        DATE NOT NULL,
                    fecha_vigencia_hasta        DATE,
                    activo                      BOOLEAN NOT NULL DEFAULT TRUE,
                    creado_en                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema, schema, schema));

        // ── 13. configuracion_mora ────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.configuracion_mora (
                    id                  BIGSERIAL PRIMARY KEY,
                    porcentaje_mensual  NUMERIC(5,2),
                    dias_gracia         INT NOT NULL DEFAULT 0,
                    tipo_calculo        VARCHAR(20) NOT NULL DEFAULT 'PORCENTAJE',
                    monto_fijo          NUMERIC(12,2),
                    activo              BOOLEAN NOT NULL DEFAULT TRUE,
                    fecha_vigencia      DATE NOT NULL,
                    creado_en           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema));

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
                    monto_base           NUMERIC(12,2) NOT NULL,
                    monto_mora           NUMERIC(12,2) NOT NULL DEFAULT 0,
                    monto_total          NUMERIC(12,2),
                    monto_pagado         NUMERIC(12,2) NOT NULL DEFAULT 0,
                    fecha_generacion     DATE NOT NULL,
                    fecha_limite_pago    DATE NOT NULL,
                    estado               VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
                    exonerado_por        BIGINT,
                    nota_exoneracion     VARCHAR(300),
                    creado_en            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema, schema));

        // ── 15. pagos ─────────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.pagos (
                    id                  BIGSERIAL PRIMARY KEY,
                    cobro_id            BIGINT NOT NULL REFERENCES %s.cobros(id),
                    usuario_id          BIGINT NOT NULL,
                    monto_pagado        NUMERIC(12,2) NOT NULL,
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
                """.formatted(schema, schema));

        // ── 16. abonos ────────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.abonos (
                    id                  BIGSERIAL PRIMARY KEY,
                    propiedad_id        BIGINT NOT NULL REFERENCES %s.propiedades(id),
                    usuario_id          BIGINT NOT NULL,
                    monto_total         NUMERIC(12,2) NOT NULL,
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
                """.formatted(schema, schema));

        // ── 17. movimientos_abono ─────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.movimientos_abono (
                    id              BIGSERIAL PRIMARY KEY,
                    abono_id        BIGINT REFERENCES %s.abonos(id),
                    cobro_id        BIGINT REFERENCES %s.cobros(id),
                    monto_aplicado  NUMERIC(12,2) NOT NULL,
                    descripcion     VARCHAR(200),
                    creado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema, schema));

        // ── 18. saldos_favor ──────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.saldos_favor (
                    id              BIGSERIAL PRIMARY KEY,
                    propiedad_id    BIGINT NOT NULL UNIQUE REFERENCES %s.propiedades(id),
                    usuario_id      BIGINT NOT NULL,
                    saldo           NUMERIC(12,2) NOT NULL DEFAULT 0,
                    creado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema));

        // ── 19. votaciones ────────────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.votaciones (
                    id                   BIGSERIAL PRIMARY KEY,
                    titulo               VARCHAR(300) NOT NULL,
                    descripcion          VARCHAR(2000),
                    tipo_votacion        VARCHAR(30) NOT NULL,
                    estado               VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
                    escala_max           INT,
                    mostrar_votantes     BOOLEAN NOT NULL DEFAULT FALSE,
                    permite_cambiar_voto BOOLEAN NOT NULL DEFAULT FALSE,
                    fecha_inicio         TIMESTAMP,
                    fecha_fin            TIMESTAMP,
                    creado_por           BIGINT NOT NULL,
                    creado_en            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema));

        // ── 20. opciones_votacion ─────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.opciones_votacion (
                    id          BIGSERIAL PRIMARY KEY,
                    votacion_id BIGINT NOT NULL REFERENCES %s.votaciones(id),
                    texto       VARCHAR(300) NOT NULL,
                    orden       INT NOT NULL DEFAULT 0
                )
                """.formatted(schema, schema));

        // ── 21. votos_residentes ──────────────────────────────────────────
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS %s.votos_residentes (
                    id               BIGSERIAL PRIMARY KEY,
                    votacion_id      BIGINT NOT NULL REFERENCES %s.votaciones(id),
                    residente_id     BIGINT NOT NULL,
                    residente_nombre VARCHAR(150),
                    opcion_id        BIGINT REFERENCES %s.opciones_votacion(id),
                    valor_numerico   INT,
                    respuesta_texto  VARCHAR(2000),
                    votado_en        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    actualizado_en   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(schema, schema, schema));
    }

    private void insertarTiposPropiedad(String schema, List<TipoPropiedadNodoDto> tipos,
                                        Long parentId, int ordenBase) {
        for (int i = 0; i < tipos.size(); i++) {
            TipoPropiedadNodoDto nodo = tipos.get(i);
            Long nuevoId = jdbcTemplate.queryForObject(
                    "INSERT INTO " + schema + ".tipos_propiedad (nombre, descripcion, parent_id, orden) " +
                    "VALUES (?, ?, ?, ?) RETURNING id",
                    Long.class,
                    nodo.nombre(), nodo.descripcion(), parentId, ordenBase + i);

            if (nodo.hijos() != null && !nodo.hijos().isEmpty()) {
                insertarTiposPropiedad(schema, nodo.hijos(), nuevoId, 0);
            }
        }
    }
}
