package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.planpago.*;
import com.backendcr.residentialcomplex.entity.*;
import com.backendcr.residentialcomplex.entity.enums.EstadoCuotaPlan;
import com.backendcr.residentialcomplex.entity.enums.EstadoPlan;
import com.backendcr.residentialcomplex.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanPagoService {

    private final PlanPagoRepository planRepo;
    private final CuotaPlanRepository cuotaRepo;
    private final ConfiguracionPlanPagoRepository configRepo;
    private final CobroRepository cobroRepo;
    private final UsuarioPropiedadRepository usuarioPropiedadRepo;
    private final PropiedadRepository propiedadRepo;
    private final UsuarioRepository usuarioRepo;

    // ── Admin ─────────────────────────────────────────────────────

    public List<PlanPagoResponse> listarTodos(String estado) {
        List<PlanPago> planes = estado != null
                ? planRepo.findByEstadoOrderByCreadoEnDesc(EstadoPlan.valueOf(estado))
                : planRepo.findAllByOrderByCreadoEnDesc();
        return planes.stream().map(p -> toResponse(p, false)).toList();
    }

    public PlanPagoResponse obtenerDetalle(Long id) {
        PlanPago plan = planRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan no encontrado"));
        return toResponse(plan, true);
    }

    @Transactional
    public PlanPagoResponse decidir(Long id, Long adminId, DecidirPlanRequest req) {
        PlanPago plan = planRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan no encontrado"));

        if (plan.getEstado() != EstadoPlan.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El plan ya fue procesado (estado: " + plan.getEstado() + ")");
        }

        if (req.aprobar()) {
            plan.setEstado(EstadoPlan.ACTIVO);
            plan.setAprobadoPor(adminId);
            plan.setFechaDecision(LocalDateTime.now());
            plan.setNotaAdmin(req.notaAdmin());
            planRepo.save(plan);
            generarCuotas(plan);
        } else {
            if (req.motivoRechazo() == null || req.motivoRechazo().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Debe indicar el motivo del rechazo");
            }
            plan.setEstado(EstadoPlan.RECHAZADO);
            plan.setMotivoRechazo(req.motivoRechazo());
            plan.setNotaAdmin(req.notaAdmin());
            plan.setFechaDecision(LocalDateTime.now());
            planRepo.save(plan);
        }
        return toResponse(plan, true);
    }

    @Transactional
    public CuotaPlanResponse marcarCuotaPagada(Long planId, Long cuotaId, String notaPago) {
        CuotaPlan cuota = cuotaRepo.findById(cuotaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuota no encontrada"));

        if (!cuota.getPlanId().equals(planId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cuota no pertenece al plan");
        }
        if (cuota.getEstado() == EstadoCuotaPlan.PAGADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuota ya fue pagada");
        }

        cuota.setEstado(EstadoCuotaPlan.PAGADA);
        cuota.setFechaPago(LocalDate.now());
        cuota.setNotaPago(notaPago);
        cuotaRepo.save(cuota);

        // Verificar si todas las cuotas del plan están pagadas
        boolean todasPagadas = !cuotaRepo.existsByPlanIdAndEstadoNot(planId, EstadoCuotaPlan.PAGADA);
        if (todasPagadas) {
            PlanPago plan = planRepo.findById(planId).orElseThrow();
            plan.setEstado(EstadoPlan.COMPLETADO);
            planRepo.save(plan);
        }

        return CuotaPlanResponse.from(cuota);
    }

    @Transactional
    public PlanPagoResponse cancelarPlan(Long id, String notaAdmin) {
        PlanPago plan = planRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan no encontrado"));

        if (plan.getEstado() == EstadoPlan.COMPLETADO || plan.getEstado() == EstadoPlan.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cancelar un plan en estado: " + plan.getEstado());
        }
        plan.setEstado(EstadoPlan.CANCELADO);
        plan.setNotaAdmin(notaAdmin);
        planRepo.save(plan);
        return toResponse(plan, true);
    }

    // ── Residente ────────────────────────────────────────────────

    @Transactional
    public PlanPagoResponse solicitar(Long residenteId, SolicitarPlanRequest req) {
        ConfiguracionPlanPago config = configRepo.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "El módulo de planes de pago no está configurado"));

        if (!config.isActivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El módulo de planes de pago no está habilitado");
        }
        if (req.numeroCuotas() > config.getMaxCuotas()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El máximo de cuotas permitidas es " + config.getMaxCuotas());
        }

        // Verificar que no tenga ya un plan activo o pendiente
        boolean tieneActivo = planRepo.existsByResidenteIdAndEstadoIn(
                residenteId, List.of(EstadoPlan.PENDIENTE, EstadoPlan.ACTIVO));
        if (tieneActivo) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya tienes un plan de pago activo o pendiente de aprobación");
        }

        // Validar que los cobros pertenecen al residente y están pendientes/vencidos
        List<Long> propIds = usuarioPropiedadRepo.findByUsuarioId(residenteId)
                .stream().map(UsuarioPropiedad::getPropiedadId).toList();

        BigDecimal montoTotal = BigDecimal.ZERO;
        for (Long cobroId : req.cobrosIds()) {
            Cobro cobro = cobroRepo.findById(cobroId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Cobro " + cobroId + " no encontrado"));
            if (!propIds.contains(cobro.getPropiedadId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No tienes acceso al cobro " + cobroId);
            }
            montoTotal = montoTotal.add(cobro.getMontoPendiente());
        }

        // Calcular recargo
        BigDecimal montoRecargo = BigDecimal.ZERO;
        if (config.isRecargoFraccionamiento() && config.getPorcentajeRecargo().compareTo(BigDecimal.ZERO) > 0) {
            montoRecargo = montoTotal
                    .multiply(config.getPorcentajeRecargo())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        }

        // Obtener propiedadId del primer cobro
        Long propiedadId = cobroRepo.findById(req.cobrosIds().get(0))
                .map(Cobro::getPropiedadId).orElse(null);

        PlanPago plan = new PlanPago();
        plan.setPropiedadId(propiedadId);
        plan.setResidenteId(residenteId);
        plan.setMontoTotalDeuda(montoTotal);
        plan.setNumeroCuotas(req.numeroCuotas());
        plan.setMontoRecargo(montoRecargo);
        plan.setMontoTotalPlan(montoTotal.add(montoRecargo));
        plan.setCobrosIncluidos(req.cobrosIds().stream()
                .map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));
        plan.setObservaciones(req.observaciones());

        // Aprobación automática
        if (config.isAprobacionAutomatica()) {
            plan.setEstado(EstadoPlan.ACTIVO);
            plan.setFechaDecision(LocalDateTime.now());
        }

        planRepo.save(plan);

        if (plan.getEstado() == EstadoPlan.ACTIVO) {
            generarCuotas(plan);
        }

        return toResponse(plan, true);
    }

    public List<PlanPagoResponse> misPlanes(Long residenteId) {
        return planRepo.findByResidenteIdOrderByCreadoEnDesc(residenteId)
                .stream().map(p -> toResponse(p, false)).toList();
    }

    public PlanPagoResponse miPlanActivo(Long residenteId) {
        return planRepo.findFirstByResidenteIdAndEstadoOrderByCreadoEnDesc(
                        residenteId, EstadoPlan.ACTIVO)
                .map(p -> toResponse(p, true))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No tienes un plan activo"));
    }

    // ── Privado ───────────────────────────────────────────────────

    private void generarCuotas(PlanPago plan) {
        BigDecimal montoPorCuota = plan.getMontoTotalPlan()
                .divide(BigDecimal.valueOf(plan.getNumeroCuotas()), 0, RoundingMode.HALF_UP);

        // La última cuota absorbe el redondeo
        BigDecimal acumulado = BigDecimal.ZERO;
        List<CuotaPlan> cuotas = new ArrayList<>();

        for (int i = 1; i <= plan.getNumeroCuotas(); i++) {
            CuotaPlan cuota = new CuotaPlan();
            cuota.setPlanId(plan.getId());
            cuota.setNumeroCuota(i);
            // Última cuota = total - acumulado
            BigDecimal monto = (i == plan.getNumeroCuotas())
                    ? plan.getMontoTotalPlan().subtract(acumulado)
                    : montoPorCuota;
            cuota.setMonto(monto);
            cuota.setFechaVencimiento(LocalDate.now().plusMonths(i));
            cuotas.add(cuota);
            acumulado = acumulado.add(monto);
        }
        cuotaRepo.saveAll(cuotas);
    }

    private PlanPagoResponse toResponse(PlanPago p, boolean conCuotas) {
        String residenteNombre = usuarioRepo.findById(p.getResidenteId())
                .map(Usuario::getNombre).orElse("N/A");
        String propiedadIdentificador = p.getPropiedadId() != null
                ? propiedadRepo.findById(p.getPropiedadId())
                .map(Propiedad::getIdentificador).orElse("N/A")
                : "N/A";

        if (!conCuotas) {
            return PlanPagoResponse.from(p, residenteNombre, propiedadIdentificador);
        }
        List<CuotaPlanResponse> cuotas = cuotaRepo.findByPlanIdOrderByNumeroCuotaAsc(p.getId())
                .stream().map(CuotaPlanResponse::from).toList();
        return PlanPagoResponse.from(p, residenteNombre, propiedadIdentificador, cuotas);
    }
}
