package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.pago.*;
import com.backendcr.residentialcomplex.entity.*;
import com.backendcr.residentialcomplex.entity.enums.*;
import com.backendcr.residentialcomplex.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.config.ColombiaTimeZone;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepo;
    private final CobroRepository cobroRepo;
    private final UsuarioRepository usuarioRepo;
    private final MovimientoAbonoRepository movimientoRepo;
    private final AbonoRepository abonoRepo;
    private final SaldoFavorRepository saldoFavorRepo;

    // ─── Consultas ─────────────────────────────────────────────────

    public List<PagoResponse> listarPorEstado(EstadoPago estado) {
        return pagoRepo.findAllByEstado(estado).stream().map(this::toResponse).toList();
    }

    public List<PagoResponse> listarPorUsuario(Long usuarioId) {
        return pagoRepo.findAllByUsuarioId(usuarioId).stream().map(this::toResponse).toList();
    }

    /**
     * Retorna todos los movimientos de pago de un cobro:
     * - Pagos directos (tipo=PAGO)
     * - Movimientos de abonos distribuidos a este cobro (tipo=ABONO)
     * Ordenados del más reciente al más antiguo.
     */
    public List<MovimientoCobroDto> getMovimientosCobro(Long cobroId) {
        List<MovimientoCobroDto> result = new ArrayList<>();

        // 1. Pagos directos al cobro
        for (Pago pago : pagoRepo.findAllByCobroId(cobroId)) {
            result.add(MovimientoCobroDto.fromPago(pago));
        }

        // 2. Movimientos de abonos distribuidos a este cobro — pre-carga en batch (evita N+1)
        List<MovimientoAbono> movimientos = movimientoRepo.findAllByCobroId(cobroId);
        Set<Long> abonoIds = movimientos.stream()
                .map(MovimientoAbono::getAbonoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Abono> abonosById = abonoRepo.findAllById(abonoIds).stream()
                .collect(Collectors.toMap(Abono::getId, a -> a));
        for (MovimientoAbono mov : movimientos) {
            if (mov.getAbonoId() != null) {
                Abono abono = abonosById.get(mov.getAbonoId());
                if (abono != null) result.add(MovimientoCobroDto.fromAbono(mov, abono));
            }
        }

        result.sort(Comparator.comparing(MovimientoCobroDto::creadoEn,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    // ─── Registro ──────────────────────────────────────────────────

    @Transactional
    public PagoResponse registrar(PagoRequest req, Long usuarioId) {
        Cobro cobro = cobroRepo.findById(req.cobroId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cobro no encontrado"));

        if (cobro.getEstado() == EstadoCobro.PAGADO || cobro.getEstado() == EstadoCobro.EXONERADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este cobro ya está " + cobro.getEstado());
        }
        if (pagoRepo.findByCobroIdAndEstado(req.cobroId(), EstadoPago.PENDIENTE_VERIFICACION).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un pago pendiente de verificación para este cobro");
        }

        Pago pago = new Pago();
        pago.setCobroId(req.cobroId());
        pago.setUsuarioId(usuarioId);
        pago.setMontoPagado(req.montoPagado());
        pago.setFechaPago(req.fechaPago());
        pago.setMetodoPago(req.metodoPago());
        pago.setReferencia(req.referencia());
        pago.setUrlComprobante(req.urlComprobante());
        pago.setEstado(EstadoPago.PENDIENTE_VERIFICACION);
        return toResponse(pagoRepo.save(pago));
    }

    // ─── Verificación (admin) ──────────────────────────────────────

    /**
     * Verifica un pago:
     * - Si montoPagado >= montoPendiente → cobro PAGADO. El exceso va a SaldoFavor.
     * - Si montoPagado < montoPendiente  → cobro PARCIAL.
     */
    @Transactional
    public PagoResponse verificar(Long id, VerificarPagoRequest req, Long adminId) {
        Pago pago = obtenerYValidar(id);
        pago.setEstado(EstadoPago.VERIFICADO);
        pago.setVerificadoPor(adminId);
        pago.setFechaVerificacion(ColombiaTimeZone.ahora());
        pagoRepo.save(pago);

        Cobro cobro = cobroRepo.findById(pago.getCobroId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cobro no encontrado"));

        BigDecimal montoPendiente = cobro.getMontoPendiente();
        BigDecimal montoPago = pago.getMontoPagado();

        if (montoPago.compareTo(montoPendiente) >= 0) {
            // Cubre el saldo completo → PAGADO
            cobro.setMontoPagado(cobro.getMontoTotal());
            cobro.setEstado(EstadoCobro.PAGADO);

            // Exceso → acumular en SaldoFavor de la propiedad
            BigDecimal exceso = montoPago.subtract(montoPendiente);
            if (exceso.compareTo(BigDecimal.ZERO) > 0) {
                SaldoFavor sf = saldoFavorRepo.findByPropiedadId(cobro.getPropiedadId())
                        .orElseGet(() -> crearSaldoFavor(cobro.getPropiedadId(), pago.getUsuarioId()));
                sf.setSaldo(sf.getSaldo().add(exceso));
                saldoFavorRepo.save(sf);
            }
        } else {
            // Pago parcial → aplicar pago al cobro
            cobro.setMontoPagado(cobro.getMontoPagado().add(montoPago));
            cobro.setEstado(EstadoCobro.PARCIAL);

            // Intentar cerrar el cobro con saldo a favor existente de la propiedad.
            // Esto cubre el caso donde el residente pagó (pendiente - saldoFavor)
            // y el saldo a favor cubre el resto.
            BigDecimal pendienteRestante = cobro.getMontoTotal().subtract(cobro.getMontoPagado());
            if (pendienteRestante.compareTo(BigDecimal.ZERO) > 0) {
                saldoFavorRepo.findByPropiedadId(cobro.getPropiedadId()).ifPresent(sf -> {
                    if (sf.getSaldo().compareTo(BigDecimal.ZERO) > 0) {
                        if (sf.getSaldo().compareTo(pendienteRestante) >= 0) {
                            // Saldo a favor cubre el resto → PAGADO
                            sf.setSaldo(sf.getSaldo().subtract(pendienteRestante));
                            cobro.setMontoPagado(cobro.getMontoTotal());
                            cobro.setEstado(EstadoCobro.PAGADO);
                        } else {
                            // Saldo a favor reduce parcialmente el pendiente
                            cobro.setMontoPagado(cobro.getMontoPagado().add(sf.getSaldo()));
                            sf.setSaldo(BigDecimal.ZERO);
                            // cobro sigue PARCIAL pero con menos pendiente
                        }
                        saldoFavorRepo.save(sf);
                    }
                });
            }
        }

        cobroRepo.save(cobro);
        return toResponse(pago);
    }

    // ─── Auto-verificación (MercadoPago webhook) ───────────────────

    /**
     * Registra y verifica en una sola transacción el pago aprobado por MercadoPago.
     *
     * Este método DEBE estar en PagoService (bean distinto a MercadoPagoService)
     * para que @Transactional abra la sesión de Hibernate ya con el TenantContext
     * correctamente seteado. Si estuviera en MercadoPagoService y se llamara desde
     * el mismo bean, Spring AOP no aplicaría el proxy y la transacción no iniciaría
     * en el momento correcto.
     */
    @Transactional
    public void registrarYVerificarPagoMP(Long cobroId, Long usuarioId, String paymentId, BigDecimal montoMP) {
        // Idempotencia: ignorar si ya está verificado
        boolean yaVerificado = pagoRepo.findAllByCobroId(cobroId).stream()
                .anyMatch(p -> p.getEstado() == EstadoPago.VERIFICADO);
        if (yaVerificado) {
            return;
        }

        Pago pago = new Pago();
        pago.setCobroId(cobroId);
        pago.setUsuarioId(usuarioId);
        pago.setMontoPagado(montoMP);
        pago.setFechaPago(ColombiaTimeZone.hoy());
        pago.setMetodoPago(MetodoPago.MERCADO_PAGO);
        pago.setReferencia("MP-" + paymentId);
        pago.setEstado(EstadoPago.PENDIENTE_VERIFICACION);
        Pago saved = pagoRepo.save(pago);

        VerificarPagoRequest req = new VerificarPagoRequest("Verificado automáticamente vía MercadoPago");
        verificar(saved.getId(), req, null);
    }

    /**
     * Método genérico para registrar y verificar pagos de pasarelas online (Wompi, Bold, etc.).
     * Funciona igual que registrarYVerificarPagoMP pero acepta cualquier MetodoPago.
     *
     * IMPORTANTE: No es @Transactional a propósito — el TenantContext debe estar seteado
     * ANTES de que comience cualquier transacción de Hibernate.
     * La lógica DB va en PagoService para que @Transactional funcione correctamente.
     */
    @Transactional
    public void registrarYVerificarPagoOnline(Long cobroId, Long usuarioId, String transaccionId,
                                               BigDecimal monto, MetodoPago metodoPago) {
        // Idempotencia: ignorar si ya está verificado
        boolean yaVerificado = pagoRepo.findAllByCobroId(cobroId).stream()
                .anyMatch(p -> p.getEstado() == EstadoPago.VERIFICADO);
        if (yaVerificado) {
            return;
        }

        Pago pago = new Pago();
        pago.setCobroId(cobroId);
        pago.setUsuarioId(usuarioId);
        pago.setMontoPagado(monto);
        pago.setFechaPago(ColombiaTimeZone.hoy());
        pago.setMetodoPago(metodoPago);
        pago.setReferencia(metodoPago.name() + "-" + transaccionId);
        pago.setEstado(EstadoPago.PENDIENTE_VERIFICACION);
        Pago saved = pagoRepo.save(pago);

        VerificarPagoRequest req = new VerificarPagoRequest(
                "Verificado automáticamente vía " + metodoPago.name()
        );
        verificar(saved.getId(), req, null);
    }

    /**
     * Verifica un pago de forma automática sin requerir un adminId.
     * Solo debe llamarse desde el webhook de MercadoPago tras confirmar que el pago fue aprobado.
     */
    @Transactional
    public void autoVerificar(Long pagoId) {
        VerificarPagoRequest req = new VerificarPagoRequest("Verificado automáticamente vía MercadoPago");
        verificar(pagoId, req, null);
    }

    // ─── Rechazo (admin) ───────────────────────────────────────────

    @Transactional
    public PagoResponse rechazar(Long id, RechazarPagoRequest req, Long adminId) {
        Pago pago = obtenerYValidar(id);
        pago.setEstado(EstadoPago.RECHAZADO);
        pago.setVerificadoPor(adminId);
        pago.setFechaVerificacion(ColombiaTimeZone.ahora());
        pago.setMotivoRechazo(req.motivoRechazo());
        return toResponse(pagoRepo.save(pago));
    }

    // ─── Helpers ───────────────────────────────────────────────────

    private Pago obtenerYValidar(Long id) {
        Pago pago = pagoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
        if (pago.getEstado() != EstadoPago.PENDIENTE_VERIFICACION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden procesar pagos en estado PENDIENTE_VERIFICACION");
        }
        return pago;
    }

    private SaldoFavor crearSaldoFavor(Long propiedadId, Long usuarioId) {
        SaldoFavor sf = new SaldoFavor();
        sf.setPropiedadId(propiedadId);
        sf.setUsuarioId(usuarioId);
        sf.setSaldo(BigDecimal.ZERO);
        return saldoFavorRepo.save(sf);
    }

    private PagoResponse toResponse(Pago p) {
        String nombre = usuarioRepo.findById(p.getUsuarioId())
                .map(Usuario::getNombre).orElse("N/A");
        return PagoResponse.from(p, nombre);
    }
}
