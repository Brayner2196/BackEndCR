package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.pago.*;
import com.backendcr.residentialcomplex.entity.*;
import com.backendcr.residentialcomplex.entity.converter.NumMesStringConverter;
import com.backendcr.residentialcomplex.entity.enums.*;
import com.backendcr.residentialcomplex.repository.*;
import com.backendcr.residentialcomplex.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.config.ColombiaTimeZone;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CobroService {

    private final TipoPropiedadRepository tipoPropiedadRepository;
    private final PeriodoCobroRepository periodoRepo;
    private final CobroRepository cobroRepo;
    private final ConfiguracionCuotaRepository cuotaRepo;
    private final ConfiguracionMoraRepository moraRepo;
    private final UsuarioPropiedadRepository usuarioPropiedadRepo;
    private final PropiedadRepository propiedadRepo;
    private final PagoRepository pagoRepo;
    private final MovimientoAbonoRepository movimientoAbonoRepo;
    private final NotificacionService notificacionService;
    private final NumMesStringConverter numMesStringConverter;
    private final TenantRepository tenantRepository;

    // ─── Períodos ──────────────────────────────────────────────

    public List<PeriodoCobroResponse> listarPeriodos() {
        return periodoRepo.findAllByOrderByAnioDescMesDesc().stream()
                .map(PeriodoCobroResponse::from).toList();
    }

    @Transactional
    public PeriodoCobroResponse abrirPeriodo(PeriodoCobroRequest req, Long adminId) {
        if (periodoRepo.findByEstado(EstadoPeriodo.ABIERTO).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un período abierto");
        }
        if (periodoRepo.existsByAnioAndMes(req.anio(), req.mes())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un período para ese mes");
        }
        PeriodoCobro p = new PeriodoCobro();
        p.setAnio(req.anio());
        p.setMes(req.mes());
        p.setFechaInicio(req.fechaInicio());
        p.setFechaFin(req.fechaFin());
        p.setFechaLimitePago(req.fechaLimitePago());
        p.setEstado(EstadoPeriodo.ABIERTO);
        p.setCreadoPor(adminId);
        PeriodoCobroResponse response = PeriodoCobroResponse.from(periodoRepo.save(p));

        String mesAnio = numMesStringConverter.convertToDatabaseColumn(req.mes())  + "/" + req.anio();
        notificacionService.enviarAAdminsTenant(
            TenantContext.getTenant(),
            "💳 Nuevo período de cobro abierto",
            "Se abrió el período de cobro " + mesAnio + " con límite de pago: " + req.fechaLimitePago().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
            java.util.Map.of("tipo", "COBRO_PERIODO", "periodoId", String.valueOf(response.id()), "route", "pagos")
        );
        return response;
    }

    @Transactional
    public PeriodoCobroResponse cerrarPeriodo(Long id) {
        PeriodoCobro p = periodoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Período no encontrado"));
        if (p.getEstado() != EstadoPeriodo.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El período no está abierto");
        }
        p.setEstado(EstadoPeriodo.CERRADO);
        return PeriodoCobroResponse.from(periodoRepo.save(p));
    }

    // ─── Cobros ──────────────────────────────────────────────

    public List<CobroResponse> listarPorPeriodo(Long periodoId) {
        return toResponseList(cobroRepo.findAllByPeriodoId(periodoId));
    }

    public List<CobroResponse> listarPorEstado(EstadoCobro estado) {
        return toResponseList(cobroRepo.findAllByEstado(estado));
    }

    public List<CobroResponse> listarPorUsuario(Long usuarioId) {
    	List<UsuarioPropiedad> propiedadIds = usuarioPropiedadRepo.findByUsuarioId(usuarioId);
        return toResponseList(cobroRepo.findAllByPropiedadIdIn(propiedadIds.stream().map(UsuarioPropiedad::getPropiedadId).toList()));
    }

    public List<CobroResponse> listarPorUsuarioYEstado(Long usuarioId, EstadoCobro estado) {
    	List<UsuarioPropiedad> propiedadIds = usuarioPropiedadRepo.findByUsuarioId(usuarioId);
        return toResponseList(cobroRepo.findAllByPropiedadIdInAndEstado(propiedadIds.stream().map(UsuarioPropiedad::getPropiedadId).toList(), estado));
    }

    /** Obtiene un único cobro validando que el usuario tenga acceso a la propiedad del cobro. */
    public CobroResponse getCobroPorIdYUsuario(Long id, Long usuarioId) {
        Cobro cobro = cobroRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cobro no encontrado"));
        boolean tieneAcceso = usuarioPropiedadRepo.findByUsuarioId(usuarioId)
                .stream()
                .anyMatch(up -> up.getPropiedadId().equals(cobro.getPropiedadId()));
        if (!tieneAcceso) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a este cobro");
        }
        return toResponse(cobro);
    }

    public List<CobroResponse> listarEspeciales() {
        return toResponseList(cobroRepo.findAllByPeriodoIdIsNull());
    }

    @Transactional
    public List<CobroResponse> generarCobros(int anio, int mes) {
        PeriodoCobro periodo = periodoRepo.findByAnioAndMes(anio, mes)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Período no encontrado para " + anio + "/" + mes));
        if (periodo.getEstado() != EstadoPeriodo.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El período no está abierto");
        }
        // Fecha de referencia para resolver cuotas vigentes: inicio del período
        LocalDate fechaRef = periodo.getFechaInicio() != null
                ? periodo.getFechaInicio()
                : LocalDate.of(anio, mes, 1);

        for (Propiedad prop : propiedadRepo.findByTipoIdIsFacturable()) {
            if (cobroRepo.existsByPeriodoIdAndPropiedadId(periodo.getId(), prop.getId())) continue;

            BigDecimal monto = resolverMonto(prop, fechaRef);
            if (monto == null) {
                Optional<TipoPropiedad> tipoProp = tipoPropiedadRepository.findById(prop.getTipoId());
                String tipoPropiedad = tipoProp.map(TipoPropiedad::getNombre).orElse("");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró configuración de cuota para el tipo de propiedad: " + tipoPropiedad);
            }
            Cobro cobro = new Cobro();
            cobro.setPeriodoId(periodo.getId());
            cobro.setPropiedadId(prop.getId());
            cobro.setConcepto(ConceptoCobro.ADMINISTRACION);
            cobro.setMontoBase(monto);
            cobro.setMontoMora(BigDecimal.ZERO);
            cobro.setMontoTotal(monto);
            cobro.setFechaGeneracion(ColombiaTimeZone.hoy());
            cobro.setFechaLimitePago(periodo.getFechaLimitePago());
            cobro.setEstado(EstadoCobro.PENDIENTE);
            cobroRepo.save(cobro);
        }
        List<CobroResponse> cobros = listarPorPeriodo(periodo.getId());

        // Notificar a todos los usuarios vinculados a cada propiedad con su cobro específico
        cobros.forEach(c -> {
            List<Long> usuariosDePropiedad = usuarioPropiedadRepo
                    .findByPropiedadId(c.propiedadId())
                    .stream()
                    .map(UsuarioPropiedad::getUsuarioId)
                    .toList();
            if (!usuariosDePropiedad.isEmpty()) {
                notificacionService.enviarAUsuarios(
                    usuariosDePropiedad,
                    "💳 Tu cobro de administración está listo",
                    "Tienes un cobro pendiente de $" + c.montoTotal() + " con límite " + c.fechaLimitePago().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                    java.util.Map.of("tipo", "COBRO_GENERADO", "cobroId", String.valueOf(c.id()), "route", "pagos")
                );
            }
        });
        return cobros;
    }

    /**
     * Crea un cobro especial (multa, sanción, etc.) directamente sobre una propiedad,
     * sin necesidad de un período de cobro abierto.
     */
    @Transactional
    public CobroResponse crearCobroEspecial(CobroEspecialRequest req, Long adminId) {
        Propiedad propiedad = propiedadRepo.findById(req.propiedadId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Propiedad no encontrada"));

        Cobro cobro = new Cobro();
        cobro.setPeriodoId(null); // cobro especial, sin período
        cobro.setPropiedadId(propiedad.getId());
        cobro.setConcepto(req.concepto());
        cobro.setDescripcion(req.descripcion());
        cobro.setMontoBase(req.monto());
        cobro.setMontoMora(BigDecimal.ZERO);
        cobro.setMontoTotal(req.monto());
        cobro.setFechaGeneracion(ColombiaTimeZone.hoy());
        cobro.setFechaLimitePago(req.fechaLimitePago());
        cobro.setEstado(EstadoCobro.PENDIENTE);
        return toResponse(cobroRepo.save(cobro));
    }

    @Transactional
    public CobroResponse exonerar(Long id, ExonerarCobroRequest req, Long adminId) {
        Cobro cobro = cobroRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cobro no encontrado"));
        cobro.setEstado(EstadoCobro.EXONERADO);
        cobro.setExoneradoPor(adminId);
        cobro.setNotaExoneracion(req.nota());
        return toResponse(cobroRepo.save(cobro));
    }

    // ─── Preview y sugerencia de próximo período ──────────────────────

    /**
     * Sugiere el año/mes del siguiente período a abrir, basándose en el último
     * período registrado (sin importar su estado).
     */
    public Map<String, Integer> sugerirProximoPeriodo() {
        List<PeriodoCobro> periodos = periodoRepo.findAllByOrderByAnioDescMesDesc();
        int anio, mes;
        if (periodos.isEmpty()) {
            LocalDate next = ColombiaTimeZone.hoy().plusMonths(1);
            anio = next.getYear();
            mes  = next.getMonthValue();
        } else {
            PeriodoCobro ultimo = periodos.get(0);
            LocalDate next = LocalDate.of(ultimo.getAnio(), ultimo.getMes(), 1).plusMonths(1);
            anio = next.getYear();
            mes  = next.getMonthValue();
        }
        return Map.of("anio", anio, "mes", mes);
    }

    /**
     * Dry-run de generarCobros: calcula cuántos cobros se generarían para (anio, mes)
     * sin persistir nada.
     */
    @Transactional(readOnly = true)
    public CobroPreviewResponse previewGenerarCobros(int anio, int mes) {
        LocalDate fechaRef = LocalDate.of(anio, mes, 1);
        List<Propiedad> props = propiedadRepo.findByTipoIdIsFacturable();

        // Cobros ya generados en este período (si el período existe)
        Optional<PeriodoCobro> periodoOpt = periodoRepo.findByAnioAndMes(anio, mes);
        Set<Long> yaGenerados = periodoOpt
                .map(p -> new HashSet<>(cobroRepo.findAllByPeriodoId(p.getId())
                        .stream().map(Cobro::getPropiedadId).toList()))
                .orElseGet(HashSet::new);

        List<String> advertencias = new ArrayList<>();
        // Precargar tipos de propiedad para lookup rápido
        Map<Long, String> tipoNombreMap = new HashMap<>();
        tipoPropiedadRepository.findAll().forEach(t -> tipoNombreMap.put(t.getId(), t.getNombre()));

        // Agrupar: key = "tipoNombre|periodicidad", value = {count, monto}
        record GrupoKey(String nombreTipo, String periodicidad) {}
        Map<GrupoKey, BigDecimal[]> grupos = new LinkedHashMap<>(); // [0]=monto, [1]=count

        int totalPendientes = 0;
        for (Propiedad prop : props) {
            if (yaGenerados.contains(prop.getId())) continue;
            BigDecimal monto = resolverMonto(prop, fechaRef);
            if (monto == null) {
                String nombreTipo = tipoNombreMap.getOrDefault(prop.getTipoId(), "?");
                advertencias.add("Sin cuota configurada para tipo: " + nombreTipo);
                continue;
            }
            // Determinar periodicidad de la cuota que aplica (buscamos la cuota vigente)
            String periodicidad = cuotaRepo.findVigenteByPropiedadId(prop.getId(), fechaRef)
                    .map(c -> c.getPeriodicidad().name())
                    .or(() -> cuotaRepo.findVigenteByTipoIdSinRango(prop.getTipoId(), fechaRef)
                            .map(c -> c.getPeriodicidad().name()))
                    .orElse("MENSUAL");

            String nombreTipo = tipoNombreMap.getOrDefault(prop.getTipoId(), "?");
            GrupoKey key = new GrupoKey(nombreTipo, periodicidad);
            grupos.computeIfAbsent(key, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            grupos.get(key)[0] = monto; // monto por unidad (último visto, todos iguales en el grupo)
            grupos.get(key)[1] = grupos.get(key)[1].add(BigDecimal.ONE); // count
            totalPendientes++;
        }

        if (!moraRepo.findFirstByActivoTrueOrderByFechaVigenciaDesc().isPresent()) {
            advertencias.add("Sin configuración de mora activa. Los cobros vencidos no generarán recargo.");
        }

        List<CobroPreviewResponse.DetalleGrupo> detalles = grupos.entrySet().stream()
                .map(e -> new CobroPreviewResponse.DetalleGrupo(
                        e.getKey().nombreTipo(),
                        e.getKey().periodicidad(),
                        e.getValue()[1].intValue(),
                        e.getValue()[0],
                        e.getValue()[0].multiply(e.getValue()[1])
                ))
                .toList();

        BigDecimal montoTotal = detalles.stream()
                .map(CobroPreviewResponse.DetalleGrupo::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CobroPreviewResponse(
                anio, mes,
                props.size(),
                yaGenerados.size(),
                totalPendientes,
                montoTotal,
                detalles,
                advertencias
        );
    }

    // ─── Job automático de mora — corre cada día a la 1 AM UTC ──────
    // Itera todos los tenants activos usando la timezone de cada uno,
    // evitando marcar como VENCIDO cobros que aún están vigentes en su zona horaria.

    @Scheduled(cron = "0 0 1 * * *")
    public void calcularMoras() {
        tenantRepository.findAll().stream()
                .filter(Tenant::isActivo)
                .forEach(tenant -> {
                    try {
                        TenantContext.setTenant(tenant.getSchemaName());
                        TenantContext.setTimezone(tenant.getTimezone() != null
                                ? tenant.getTimezone() : "America/Bogota");
                        calcularMorasParaTenant();
                    } finally {
                        TenantContext.clear();
                    }
                });
    }

    @Transactional
    public void calcularMorasParaTenant() {
        LocalDate hoy = LocalDate.now(ZoneId.of(TenantContext.getTimezone()));
        List<Cobro> vencidos = cobroRepo.findAllByEstadoInAndFechaLimitePagoBefore(
                List.of(EstadoCobro.PENDIENTE, EstadoCobro.PARCIAL), hoy);
        ConfiguracionMora config = moraRepo.findFirstByActivoTrueOrderByFechaVigenciaDesc().orElse(null);

        for (Cobro cobro : vencidos) {
            if (config != null) {
                BigDecimal mora;
                if (config.getTipoCalculo() == TipoCalculoMora.PORCENTAJE) {
                    mora = cobro.getMontoBase()
                            .multiply(config.getPorcentajeMensual())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                } else {
                    mora = config.getMontoFijo();
                }
                cobro.setMontoMora(mora);
                cobro.setMontoTotal(cobro.getMontoBase().add(mora));
            }
            cobro.setEstado(EstadoCobro.VENCIDO);
            cobroRepo.save(cobro);
        }
    }

    // ─── Historial paginado del residente (infinite scroll) ───────

    /**
     * Devuelve un Page<CobroResponse> con TODOS los cobros del usuario
     * (cualquier estado), ordenados por fechaGeneracion desc.
     * Usar con page=0&size=5 para el primer cargue y aumentar page en scroll.
     */
    @Transactional(readOnly = true)
    public Page<CobroResponse> listarHistorialPaginado(Long usuarioId, Pageable pageable) {
        List<Long> propiedadIds = usuarioPropiedadRepo.findByUsuarioId(usuarioId)
                .stream().map(UsuarioPropiedad::getPropiedadId).toList();
        if (propiedadIds.isEmpty()) return Page.empty(pageable);
        Page<Cobro> page = cobroRepo.findAllByPropiedadIdInOrderByFechaGeneracionDesc(propiedadIds, pageable);
        return page.map(c -> toResponseList(List.of(c)).get(0));
    }

    // ─── Estado de cuenta del residente ───────────────────────────

    public EstadoCuentaResponse estadoCuenta(Long usuarioId) {
    	List<Long> propiedadIds = usuarioPropiedadRepo.findByUsuarioId(usuarioId)
				.stream().map(UsuarioPropiedad::getPropiedadId).toList();
        List<Cobro> activos = cobroRepo.findAllByPropiedadIdIn(propiedadIds).stream()
                .filter(c -> c.getEstado() == EstadoCobro.PENDIENTE
                          || c.getEstado() == EstadoCobro.PARCIAL
                          || c.getEstado() == EstadoCobro.VENCIDO)
                .toList();

        BigDecimal totalPendiente = activos.stream()
                .filter(c -> c.getEstado() == EstadoCobro.PENDIENTE || c.getEstado() == EstadoCobro.PARCIAL)
                .map(Cobro::getMontoPendiente).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVencido = activos.stream()
                .filter(c -> c.getEstado() == EstadoCobro.VENCIDO)
                .map(Cobro::getMontoPendiente).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new EstadoCuentaResponse(
                totalPendiente, totalVencido,
                (int) activos.stream().filter(c -> c.getEstado() == EstadoCobro.VENCIDO).count(),
                (int) activos.stream().filter(c -> c.getEstado() == EstadoCobro.PENDIENTE
                                                || c.getEstado() == EstadoCobro.PARCIAL).count(),
                null,
                toResponseList(activos)); 
    }

    // ─── Helpers de resolución de cuota ──────────────────────────────

    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)\\s*$");

    /**
     * Resuelve el monto de cuota vigente en {@code fechaRef} para la propiedad,
     * siguiendo esta prioridad:
     * 1. Cuota específica para la propiedad exacta (propiedadId).
     * 2. Cuota con rango numérico del tipo:
     *    - Si tipoPropiedadCondicionId != null → el número se extrae del ancestro de ese tipo.
     *    - Si tipoPropiedadCondicionId == null → el número se extrae del identificador propio.
     * 3. Cuota general del tipo (sin rango).
     */
    private BigDecimal resolverMonto(Propiedad prop, LocalDate fechaRef) {
        // 1. Cuota específica de esta propiedad vigente en la fecha
        Optional<BigDecimal> exacto = cuotaRepo
                .findVigenteByPropiedadId(prop.getId(), fechaRef)
                .map(ConfiguracionCuota::getMonto);
        if (exacto.isPresent()) return exacto.get();

        // 2. Cuotas con rango numérico del tipo vigentes en la fecha
        List<ConfiguracionCuota> rangos = cuotaRepo
                .findVigentesByTipoIdConRango(prop.getTipoId(), fechaRef);

        if (!rangos.isEmpty()) {
            for (ConfiguracionCuota c : rangos) {
                Integer num = resolverNumeroParaCondicion(prop, c.getTipoPropiedadCondicionId());
                if (num == null) continue;
                int hasta = c.getNumeroHasta() != null ? c.getNumeroHasta() : Integer.MAX_VALUE;
                if (c.getNumeroDesde() <= num && num <= hasta) {
                    return c.getMonto();
                }
            }
        }

        // 3. Cuota general del tipo (sin rango) vigente en la fecha
        return cuotaRepo
                .findVigenteByTipoIdSinRango(prop.getTipoId(), fechaRef)
                .map(ConfiguracionCuota::getMonto)
                .orElse(null);
    }

    /**
     * Determina el número a usar para evaluar la regla de rango.
     * - Si condicionTipoId es null → usa el identificador numérico de la propia propiedad.
     * - Si condicionTipoId tiene valor → sube por la jerarquía hasta encontrar un ancestro
     *   cuyo tipoId coincida, y usa su identificador numérico.
     *   Si no hay ningún ancestro de ese tipo, retorna null (regla no aplica).
     */
    private Integer resolverNumeroParaCondicion(Propiedad prop, Long condicionTipoId) {
        if (condicionTipoId == null) {
            return extraerNumero(prop.getIdentificador());
        }
        // Subir por la jerarquía buscando el ancestro del tipo indicado
        Long parentId = prop.getParentId();
        while (parentId != null) {
            Optional<Propiedad> ancestroOpt = propiedadRepo.findById(parentId);
            if (ancestroOpt.isEmpty()) break;
            Propiedad ancestro = ancestroOpt.get();
            if (condicionTipoId.equals(ancestro.getTipoId())) {
                return extraerNumero(ancestro.getIdentificador());
            }
            parentId = ancestro.getParentId();
        }
        return null; // no se encontró el tipo ancestro → regla no aplica
    }

    /** Extrae el último número del identificador (ej. "Piso 10" → 10, "42" → 42). */
    private Integer extraerNumero(String identificador) {
        if (identificador == null) return null;
        Matcher m = TRAILING_NUMBER.matcher(identificador.trim());
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    // ─── Helpers de mapeo a response ─────────────────────────────────

    /**
     * Convierte una lista de Cobro en CobroResponse usando batch loading.
     */
    @Transactional(readOnly = true)
    private List<CobroResponse> toResponseList(List<Cobro> cobros) {
        if (cobros.isEmpty()) return List.of();

        Set<Long> cobroIds   = cobros.stream().map(Cobro::getId).collect(Collectors.toSet());
        Set<Long> propIds    = cobros.stream().map(Cobro::getPropiedadId).collect(Collectors.toSet());
        // periodoId puede ser null en cobros especiales — filtramos
        Set<Long> periodoIds = cobros.stream().map(Cobro::getPeriodoId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        // Jerarquía de propiedades (múltiples pasadas para subir niveles)
        Map<Long, Propiedad> propMap = new HashMap<>();
        List<Long> toFetch = new ArrayList<>(propIds);
        while (!toFetch.isEmpty()) {
            List<Propiedad> fetched = propiedadRepo.findAllById(toFetch);
            toFetch = new ArrayList<>();
            for (Propiedad p : fetched) {
                propMap.put(p.getId(), p);
                if (p.getParentId() != null && !propMap.containsKey(p.getParentId())) {
                    toFetch.add(p.getParentId());
                }
            }
        }

        Set<Long> tipoIds = propMap.values().stream().map(Propiedad::getTipoId).collect(Collectors.toSet());
        Map<Long, TipoPropiedad> tipoMap = tipoPropiedadRepository.findAllById(tipoIds)
                .stream().collect(Collectors.toMap(TipoPropiedad::getId, t -> t));

        Map<Long, PeriodoCobro> periodoMap = periodoIds.isEmpty()
                ? Map.of()
                : periodoRepo.findAllById(periodoIds)
                        .stream().collect(Collectors.toMap(PeriodoCobro::getId, p -> p));

        Set<Long> conPagos     = pagoRepo.findCobroIdsWithPagos(cobroIds);
        Set<Long> conMovAbonos = movimientoAbonoRepo.findCobroIdsWithMovimientos(cobroIds);

        return cobros.stream().map(c -> {
            Propiedad prop = propMap.get(c.getPropiedadId());
            String descripcion = prop != null
                    ? construirPathTextoDesdeMap(prop, propMap, tipoMap) : "N/A";
            PeriodoCobro periodo = c.getPeriodoId() != null ? periodoMap.get(c.getPeriodoId()) : null;
            boolean tieneMovimientos = conPagos.contains(c.getId()) || conMovAbonos.contains(c.getId());
            EstadoCobro estadoCobro = c.getEstado();
            if (c.getEstado() != null
                    && (c.getEstado() == EstadoCobro.PENDIENTE || c.getEstado() == EstadoCobro.PARCIAL)
                    && c.getFechaLimitePago().isBefore(ColombiaTimeZone.hoy())) {
                estadoCobro = EstadoCobro.VENCIDO;
            }

            return new CobroResponse(
                    c.getId(), c.getPeriodoId(),
                    periodo != null ? periodo.getAnio() : null,
                    periodo != null ? periodo.getMes() : null,
                    c.getPropiedadId(), descripcion,
                    c.getConcepto(), c.getDescripcion(),
                    c.getMontoBase(), c.getMontoMora(), c.getMontoTotal(),
                    c.getMontoPagado(), c.getMontoPendiente(),
                    c.getFechaGeneracion(), c.getFechaLimitePago(),
                    estadoCobro, tieneMovimientos);
        }).toList();
    }

    private String construirPathTextoDesdeMap(Propiedad hoja,
                                               Map<Long, Propiedad> propMap,
                                               Map<Long, TipoPropiedad> tipoMap) {
        List<String> partes = new ArrayList<>();
        Propiedad actual = hoja;
        while (actual != null) {
            TipoPropiedad tipo = tipoMap.get(actual.getTipoId());
            String nombreTipo = tipo != null ? tipo.getNombre() : "?";
            partes.add(0, nombreTipo + " " + actual.getIdentificador());
            if (actual.getParentId() == null) break;
            actual = propMap.get(actual.getParentId());
        }
        return String.join(" / ", partes);
    }

    private CobroResponse toResponse(Cobro c) {
        String descripcionPropiedad = propiedadRepo.findById(c.getPropiedadId())
                .map(this::construirPathTexto).orElse("N/A");
        PeriodoCobro periodo = c.getPeriodoId() != null
                ? periodoRepo.findById(c.getPeriodoId()).orElse(null) : null;
        boolean tieneMovimientos = pagoRepo.existsByCobroId(c.getId())
                || movimientoAbonoRepo.existsByCobroId(c.getId());
        return new CobroResponse(
                c.getId(), c.getPeriodoId(),
                periodo != null ? periodo.getAnio() : 0,
                periodo != null ? periodo.getMes() : 0,
                c.getPropiedadId(), descripcionPropiedad,
                c.getConcepto(), c.getDescripcion(),
                c.getMontoBase(), c.getMontoMora(), c.getMontoTotal(),
                c.getMontoPagado(), c.getMontoPendiente(),
                c.getFechaGeneracion(), c.getFechaLimitePago(),
                c.getEstado(), tieneMovimientos);
    }

    private String construirPathTexto(Propiedad hoja) {
        List<String> partes = new ArrayList<>();
        Propiedad actual = hoja;
        while (actual != null) {
            String nombreTipo = tipoPropiedadRepository.findById(actual.getTipoId())
                    .map(TipoPropiedad::getNombre).orElse("?");
            partes.add(0, nombreTipo + " " + actual.getIdentificador());
            if (actual.getParentId() == null) break;
            actual = propiedadRepo.findById(actual.getParentId()).orElse(null);
        }
        return String.join(" / ", partes);
    }
}
