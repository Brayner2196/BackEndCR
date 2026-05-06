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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        return cobroRepo.findAllByPeriodoId(periodoId).stream().map(this::toResponse).toList();
    }

    public List<CobroResponse> listarPorEstado(EstadoCobro estado) {
        return cobroRepo.findAllByEstado(estado).stream().map(this::toResponse).toList();
    }

    public List<CobroResponse> listarPorUsuario(Long usuarioId) {
        return cobroRepo.findAllByUsuarioId(usuarioId).stream().map(this::toResponse).toList();
    }

    public List<CobroResponse> listarPorUsuarioYEstado(Long usuarioId, EstadoCobro estado) {
        return cobroRepo.findAllByUsuarioIdAndEstado(usuarioId, estado).stream().map(this::toResponse).toList();
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
                activos.stream().map(this::toResponse).toList());
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
