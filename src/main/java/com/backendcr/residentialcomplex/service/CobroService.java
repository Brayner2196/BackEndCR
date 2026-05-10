package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.pago.*;
import com.backendcr.residentialcomplex.entity.*;
import com.backendcr.residentialcomplex.entity.enums.*;
import com.backendcr.residentialcomplex.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
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
    private final UsuarioRepository usuarioRepo;
    private final PagoRepository pagoRepo;
    private final MovimientoAbonoRepository movimientoAbonoRepo;

    // ─── Períodos ──────────────────────────────────────────────

    public List<PeriodoCobroResponse> listarPeriodos() {
        return periodoRepo.findAllByOrderByAnioDescMesDesc().stream()
                .map(PeriodoCobroResponse::from).toList();
    }

    @Transactional
    public PeriodoCobroResponse abrirPeriodo(PeriodoCobroRequest req) {
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
        return PeriodoCobroResponse.from(periodoRepo.save(p));
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
        return toResponseList(cobroRepo.findAllByUsuarioId(usuarioId));
    }

    public List<CobroResponse> listarPorUsuarioYEstado(Long usuarioId, EstadoCobro estado) {
        return toResponseList(cobroRepo.findAllByUsuarioIdAndEstado(usuarioId, estado));
    }

    @Transactional
    public List<CobroResponse> generarCobros(int anio, int mes) {
        PeriodoCobro periodo = periodoRepo.findByAnioAndMes(anio, mes)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Período no encontrado para " + anio + "/" + mes));
        if (periodo.getEstado() != EstadoPeriodo.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El período no está abierto");
        }

        for (Propiedad prop : propiedadRepo.findByTipoIdIsFacturable() ) {
            if (cobroRepo.existsByPeriodoIdAndPropiedadId(periodo.getId(), prop.getId())) continue;
            BigDecimal monto = resolverMonto(prop);
            if (monto == null) {
            	Optional<TipoPropiedad> tipoProp = tipoPropiedadRepository.findById(prop.getTipoId());
            	String tipoPropiedad =  tipoProp.isPresent() ? tipoProp.get().getNombre() : "";
            	throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se logro encontrar una configuracion de cuota para determinar el valor del cobro para el tipo de propiedad: " + tipoPropiedad);
            }

            Long usuarioId = usuarioPropiedadRepo.findOptionalByPropiedadId(prop.getId())
                    .map(UsuarioPropiedad::getUsuarioId).orElse(null);

            Cobro cobro = new Cobro();
            cobro.setPeriodoId(periodo.getId());
            cobro.setPropiedadId(prop.getId());
            cobro.setUsuarioId(usuarioId);
            cobro.setConcepto(ConceptoCobro.ADMINISTRACION);
            cobro.setMontoBase(monto);
            cobro.setMontoMora(BigDecimal.ZERO);
            cobro.setMontoTotal(monto);
            cobro.setFechaGeneracion(LocalDate.now());
            cobro.setFechaLimitePago(periodo.getFechaLimitePago());
            cobro.setEstado(EstadoCobro.PENDIENTE);
            cobroRepo.save(cobro);
        }
        return listarPorPeriodo(periodo.getId());
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

    // ─── Job automático de mora — corre cada día a la 1 AM ───────────

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void calcularMoras() {
        List<Cobro> vencidos = cobroRepo.findAllByEstadoInAndFechaLimitePagoBefore(
                List.of(EstadoCobro.PENDIENTE, EstadoCobro.PARCIAL), LocalDate.now());
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

    // ─── Estado de cuenta del residente ───────────────────────────

    public EstadoCuentaResponse estadoCuenta(Long usuarioId) {
        List<Cobro> activos = cobroRepo.findAllByUsuarioId(usuarioId).stream()
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

    // ─── Helpers ──────────────────────────────────────────────

    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)\\s*$");

    /**
     * Resuelve el monto de cuota para una propiedad siguiendo esta prioridad:
     * 1. Cuota específica para la propiedad exacta (por propiedadId)
     * 2. Cuota por rango numérico dentro del tipo (numeroDesde..numeroHasta)
     * 3. Cuota general del tipo (sin rango)
     */
    private BigDecimal resolverMonto(Propiedad prop) {
        // 1. Match exacto por propiedad
        Optional<BigDecimal> exacto = cuotaRepo
                .findByPropiedadIdAndActivoTrue(prop.getId())
                .map(ConfiguracionCuota::getMonto);
        if (exacto.isPresent()) return exacto.get();

        // 2. Match por rango numérico del tipo
        Integer numPropiedad = extraerNumero(prop.getIdentificador());
        if (numPropiedad != null) {
            List<ConfiguracionCuota> rangos = cuotaRepo
                    .findByTipoPropiedadIdAndNumeroDesdeIsNotNullAndActivoTrue(prop.getTipoId());
            Optional<BigDecimal> rango = rangos.stream()
                    .filter(c -> c.getNumeroDesde() <= numPropiedad
                              && numPropiedad <= c.getNumeroHasta())
                    .map(ConfiguracionCuota::getMonto)
                    .findFirst();
            if (rango.isPresent()) return rango.get();
        }

        // 3. Cuota general del tipo (sin rango)
        return cuotaRepo
                .findByTipoPropiedadIdAndNumeroDesdeIsNullAndActivoTrue(prop.getTipoId())
                .map(ConfiguracionCuota::getMonto)
                .orElse(null);
    }

    /** Extrae el último número del identificador de la propiedad (ej. "Apto 5" → 5, "42" → 42). */
    private Integer extraerNumero(String identificador) {
        if (identificador == null) return null;
        Matcher m = TRAILING_NUMBER.matcher(identificador.trim());
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    /**
     * Convierte una lista de Cobro en CobroResponse usando batch loading.
     * Reduce de 5N+1 queries individuales a ~8 queries totales.
     */
    @Transactional(readOnly = true)
    private List<CobroResponse> toResponseList(List<Cobro> cobros) {
        if (cobros.isEmpty()) return List.of();

        // ── 1. Recolectar IDs únicos ─────────────────────────────────────
        Set<Long> cobroIds    = cobros.stream().map(Cobro::getId).collect(Collectors.toSet());
        Set<Long> propIds     = cobros.stream().map(Cobro::getPropiedadId).collect(Collectors.toSet());
        Set<Long> periodoIds  = cobros.stream().map(Cobro::getPeriodoId).collect(Collectors.toSet());
        Set<Long> usuarioIds  = cobros.stream().map(Cobro::getUsuarioId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        // ── 2. Cargar jerarquía de propiedades en múltiples pasadas ───────
        //    Cada pasada fetchea un nivel de padres no cargados aún.
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

        // ── 3. Cargar tipos de propiedad usados en la jerarquía ───────────
        Set<Long> tipoIds = propMap.values().stream().map(Propiedad::getTipoId).collect(Collectors.toSet());
        Map<Long, TipoPropiedad> tipoMap = tipoPropiedadRepository.findAllById(tipoIds)
                .stream().collect(Collectors.toMap(TipoPropiedad::getId, t -> t));

        // ── 4. Batch fetch usuarios y períodos ────────────────────────────
        Map<Long, Usuario> usuarioMap = usuarioRepo.findAllById(usuarioIds)
                .stream().collect(Collectors.toMap(Usuario::getId, u -> u));
        Map<Long, PeriodoCobro> periodoMap = periodoRepo.findAllById(periodoIds)
                .stream().collect(Collectors.toMap(PeriodoCobro::getId, p -> p));

        // ── 5. Batch check tieneMovimientos (2 queries IN en lugar de 2N) ─
        Set<Long> conPagos      = pagoRepo.findCobroIdsWithPagos(cobroIds);
        Set<Long> conMovAbonos  = movimientoAbonoRepo.findCobroIdsWithMovimientos(cobroIds);

        // ── 6. Mapear sin ninguna query adicional ─────────────────────────
        return cobros.stream().map(c -> {
            Propiedad prop = propMap.get(c.getPropiedadId());
            String descripcion = prop != null
                    ? construirPathTextoDesdeMap(prop, propMap, tipoMap) : "N/A";
            String nombreUsuario = c.getUsuarioId() != null
                    ? usuarioMap.getOrDefault(c.getUsuarioId(), null) != null
                        ? usuarioMap.get(c.getUsuarioId()).getNombre() : "N/A"
                    : "N/A";
            PeriodoCobro periodo = periodoMap.get(c.getPeriodoId());
            boolean tieneMovimientos = conPagos.contains(c.getId()) || conMovAbonos.contains(c.getId());
            EstadoCobro estadoCobro;
			if (c.getEstado() != null && (c.getEstado().equals(EstadoCobro.PENDIENTE) || c.getEstado().equals(EstadoCobro.PARCIAL)  && c.getFechaLimitePago().isAfter(LocalDate.now())))
				estadoCobro = EstadoCobro.VENCIDO;
			else
				estadoCobro = c.getEstado();

            return new CobroResponse(
                    c.getId(), c.getPeriodoId(),
                    periodo != null ? periodo.getAnio() : 0,
                    periodo != null ? periodo.getMes() : 0,
                    c.getPropiedadId(), descripcion,
                    c.getUsuarioId(), nombreUsuario,
                    c.getConcepto(), c.getDescripcion(),
                    c.getMontoBase(), c.getMontoMora(), c.getMontoTotal(),
                    c.getMontoPagado(), c.getMontoPendiente(),
                    c.getFechaGeneracion(), c.getFechaLimitePago(),
                    estadoCobro, tieneMovimientos);
        }).toList();
    }

    /** Construye el path usando mapas precargados — sin queries adicionales. */
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
        String nombreUsuario = c.getUsuarioId() != null
                ? usuarioRepo.findById(c.getUsuarioId()).map(Usuario::getNombre).orElse("N/A") : "N/A";
        PeriodoCobro periodo = periodoRepo.findById(c.getPeriodoId()).orElse(null);
        boolean tieneMovimientos = pagoRepo.existsByCobroId(c.getId())
                || movimientoAbonoRepo.existsByCobroId(c.getId());
        return new CobroResponse(
                c.getId(), c.getPeriodoId(),
                periodo != null ? periodo.getAnio() : 0,
                periodo != null ? periodo.getMes() : 0,
                c.getPropiedadId(),
                descripcionPropiedad,
                c.getUsuarioId(),
                nombreUsuario,
                c.getConcepto(),
                c.getDescripcion(),
                c.getMontoBase(),
                c.getMontoMora(),
                c.getMontoTotal(),
                c.getMontoPagado(),
                c.getMontoPendiente(),
                c.getFechaGeneracion(),
                c.getFechaLimitePago(),
                c.getEstado(),
                tieneMovimientos);
    }
    
    private String construirPathTexto(Propiedad hoja) {
        List<String> partes = new ArrayList<>();
        Propiedad actual = hoja;
        while (actual != null) {
            String nombreTipo = tipoPropiedadRepository.findById(actual.getTipoId())
                    .map(TipoPropiedad::getNombre)
                    .orElse("?");
            partes.add(0, nombreTipo + " " + actual.getIdentificador());
            if (actual.getParentId() == null) break;
            actual = propiedadRepo.findById(actual.getParentId()).orElse(null);
        }
        return String.join(" / ", partes);
    }
}
