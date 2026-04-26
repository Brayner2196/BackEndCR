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
import java.util.List;
import java.util.Optional;

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

    // ─── Períodos ──────────────────────────────────────────────────

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

    // ─── Cobros ────────────────────────────────────────────────────

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

        for (Propiedad prop : propiedadRepo.findAll()) {
            if (cobroRepo.existsByPeriodoIdAndPropiedadId(periodo.getId(), prop.getId())) continue;
            BigDecimal monto = resolverMonto(prop);
            if (monto == null) continue;

            Long usuarioId = usuarioPropiedadRepo.findByPropiedadIdAndEsPrincipalTrue(prop.getId())
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

    // ─── Job automático de mora — corre cada día a la 1 AM ────────

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void calcularMoras() {
        List<Cobro> vencidos = cobroRepo.findAllByEstadoAndFechaLimitePagoBefore(
                EstadoCobro.PENDIENTE, LocalDate.now());
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
                .filter(c -> c.getEstado() == EstadoCobro.PENDIENTE || c.getEstado() == EstadoCobro.VENCIDO)
                .toList();

        BigDecimal totalPendiente = activos.stream().filter(c -> c.getEstado() == EstadoCobro.PENDIENTE)
                .map(Cobro::getMontoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVencido = activos.stream().filter(c -> c.getEstado() == EstadoCobro.VENCIDO)
                .map(Cobro::getMontoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new EstadoCuentaResponse(
                totalPendiente, totalVencido,
                (int) activos.stream().filter(c -> c.getEstado() == EstadoCobro.VENCIDO).count(),
                (int) activos.stream().filter(c -> c.getEstado() == EstadoCobro.PENDIENTE).count(),
                null,
                activos.stream().map(this::toResponse).toList());
    }

    // ─── Helpers ──────────────────────────────────────────────────

    private BigDecimal resolverMonto(Propiedad prop) {
        return cuotaRepo.findByPropiedadIdAndActivoTrue(prop.getId())
                .map(ConfiguracionCuota::getMonto)
                .orElseGet(() -> cuotaRepo.findByTipoPropiedadIdAndActivoTrue(prop.getTipoId())
                        .map(ConfiguracionCuota::getMonto).orElse(null));
    }

    private CobroResponse toResponse(Cobro c) {
        String descripcionPropiedad = propiedadRepo.findById(c.getPropiedadId())
                .map(Propiedad::getIdentificador).orElse("N/A");
        
        String nombreUsuario = c.getUsuarioId() != null
                ? usuarioRepo.findById(c.getUsuarioId()).map(Usuario::getNombre).orElse("N/A") : "N/A";
        PeriodoCobro periodo = periodoRepo.findById(c.getPeriodoId()).orElse(null);
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
                c.getFechaGeneracion(), 
                c.getFechaLimitePago(), 
                c.getEstado());
    }
}
