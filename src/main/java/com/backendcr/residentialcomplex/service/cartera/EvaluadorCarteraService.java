package com.backendcr.residentialcomplex.service.cartera;

import com.backendcr.residentialcomplex.config.ColombiaTimeZone;
import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.cartera.EstadoCarteraResponse;
import com.backendcr.residentialcomplex.entity.*;
import com.backendcr.residentialcomplex.entity.enums.EstadoCobro;
import com.backendcr.residentialcomplex.repository.*;
import com.backendcr.residentialcomplex.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Motor de cartera: calcula las métricas de una propiedad, resuelve su estado
 * según la configuración del tenant y persiste el snapshot + historial.
 */
@Service
@RequiredArgsConstructor
public class EvaluadorCarteraService {

    private final CobroRepository cobroRepo;
    private final PropiedadRepository propiedadRepo;
    private final EstadoCarteraRepository estadoRepo;
    private final ReglaEstadoCarteraRepository reglaRepo;
    private final CondicionReglaRepository condicionRepo;
    private final EstadoCarteraPropiedadRepository snapshotRepo;
    private final HistorialEstadoCarteraRepository historialRepo;
    private final EvaluadorCondiciones evaluador;
    private final TenantRepository tenantRepository;

    private static final List<EstadoCobro> ESTADOS_DEUDA =
            List.of(EstadoCobro.PENDIENTE, EstadoCobro.PARCIAL, EstadoCobro.VENCIDO);

    // ─── Cálculo de métricas ──────────────────────────────────────────────

    public MetricasCartera calcularMetricas(Long propiedadId) {
        LocalDate hoy = ColombiaTimeZone.hoy();
        List<Cobro> activos = cobroRepo.findAllByPropiedadId(propiedadId).stream()
                .filter(c -> ESTADOS_DEUDA.contains(c.getEstado()))
                .toList();

        BigDecimal montoAdeudado = activos.stream()
                .map(Cobro::getMontoPendiente)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Cobro> vencidos = activos.stream()
                .filter(c -> c.getFechaLimitePago() != null
                        && c.getFechaLimitePago().isBefore(hoy))
                .toList();

        int diasVencidoMax = vencidos.stream()
                .mapToInt(c -> (int) ChronoUnit.DAYS.between(c.getFechaLimitePago(), hoy))
                .max().orElse(0);

        int numCobrosVencidos = vencidos.size();
        int numPeriodosVencidos = (int) vencidos.stream()
                .map(Cobro::getPeriodoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .size();

        return new MetricasCartera(diasVencidoMax, montoAdeudado, numCobrosVencidos, numPeriodosVencidos);
    }

    // ─── Resolución del estado ────────────────────────────────────────────

    /**
     * Devuelve el estado más severo cuyas reglas se cumplan; si ninguno aplica,
     * el estado base (es_positivo). Null si el tenant no configuró estados.
     */
    public EstadoCartera resolverEstado(MetricasCartera metricas) {
        List<EstadoCartera> estados = estadoRepo.findByActivoTrueOrderBySeveridadDesc();
        if (estados.isEmpty()) return null;

        for (EstadoCartera estado : estados) {
            if (estado.isEsPositivo()) continue; // el base es el fallback
            List<ReglaEstadoCartera> reglas =
                    reglaRepo.findByEstadoCarteraIdAndActivaTrueOrderByOrdenAsc(estado.getId());
            for (ReglaEstadoCartera regla : reglas) {
                List<CondicionRegla> condiciones = condicionRepo.findByReglaId(regla.getId());
                if (evaluador.cumpleRegla(regla, condiciones, metricas)) {
                    return estado;
                }
            }
        }
        return estadoRepo.findFirstByEsPositivoTrueAndActivoTrue().orElse(null);
    }

    // ─── Recálculo y persistencia ─────────────────────────────────────────

    /** Recalcula y persiste el estado de cartera de una propiedad. */
    @Transactional
    public void recalcular(Long propiedadId) {
        MetricasCartera m = calcularMetricas(propiedadId);
        EstadoCartera nuevo = resolverEstado(m);
        if (nuevo == null) return; // tenant sin configuración → no se fuerza nada

        EstadoCarteraPropiedad snapshot = snapshotRepo.findByPropiedadId(propiedadId).orElse(null);
        Long anteriorId = snapshot != null ? snapshot.getEstadoCarteraId() : null;

        if (snapshot == null) {
            snapshot = new EstadoCarteraPropiedad();
            snapshot.setPropiedadId(propiedadId);
        }
        snapshot.setEstadoCarteraId(nuevo.getId());
        snapshot.setDiasVencidoMax(m.diasVencidoMax());
        snapshot.setMontoAdeudado(m.montoAdeudado());
        snapshotRepo.save(snapshot);

        // Historial solo cuando cambia el estado
        if (!Objects.equals(anteriorId, nuevo.getId())) {
            HistorialEstadoCartera h = new HistorialEstadoCartera();
            h.setPropiedadId(propiedadId);
            h.setEstadoAnteriorId(anteriorId);
            h.setEstadoNuevoId(nuevo.getId());
            h.setDiasVencidoMax(m.diasVencidoMax());
            h.setMontoAdeudado(m.montoAdeudado());
            historialRepo.save(h);
        }
    }

    /** Recalcula todas las propiedades facturables del tenant actual. */
    @Transactional
    public void recalcularTodasDelTenant() {
        if (estadoRepo.countByActivoTrue() == 0) return; // sin configuración
        for (Propiedad prop : propiedadRepo.findByTipoIdIsFacturable()) {
            recalcular(prop.getId());
        }
    }

    /** Consulta el estado de cartera vigente de una propiedad (sin recalcular). */
    @Transactional(readOnly = true)
    public EstadoCarteraResponse consultarEstado(Long propiedadId) {
        EstadoCarteraPropiedad snap = snapshotRepo.findByPropiedadId(propiedadId).orElse(null);
        if (snap == null) {
            return new EstadoCarteraResponse(propiedadId, null, null, null, true, 0, BigDecimal.ZERO, null);
        }
        EstadoCartera e = estadoRepo.findById(snap.getEstadoCarteraId()).orElse(null);
        return new EstadoCarteraResponse(
                propiedadId,
                e != null ? e.getCodigo() : null,
                e != null ? e.getNombre() : null,
                e != null ? e.getColor() : null,
                e != null && e.isEsPositivo(),
                snap.getDiasVencidoMax(),
                snap.getMontoAdeudado(),
                snap.getCalculadoEn());
    }

    /** Recalcula y devuelve el estado resultante. */
    @Transactional
    public EstadoCarteraResponse recalcularYConsultar(Long propiedadId) {
        recalcular(propiedadId);
        return consultarEstado(propiedadId);
    }

    // ─── Job diario — corre tras el cálculo de moras (1 AM) ───────────────

    @Scheduled(cron = "0 30 1 * * *")
    public void recalcularTodosLosTenants() {
        tenantRepository.findAll().stream()
                .filter(Tenant::isActivo)
                .forEach(tenant -> {
                    try {
                        TenantContext.setTenant(tenant.getSchemaName());
                        TenantContext.setTimezone(tenant.getTimezone() != null
                                ? tenant.getTimezone() : "America/Bogota");
                        recalcularTodasDelTenant();
                    } finally {
                        TenantContext.clear();
                    }
                });
    }
}
