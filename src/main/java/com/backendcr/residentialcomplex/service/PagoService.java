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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

        // 2. Movimientos de abonos distribuidos a este cobro
        for (MovimientoAbono mov : movimientoRepo.findAllByCobroId(cobroId)) {
            if (mov.getAbonoId() != null) {
                abonoRepo.findById(mov.getAbonoId())
                        .ifPresent(abono -> result.add(MovimientoCobroDto.fromAbono(mov, abono)));
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
        pago.setNotas(req.notas());
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
        pago.setFechaVerificacion(LocalDateTime.now());
        if (req.notas() != null) pago.setNotas(req.notas());
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
            // Pago parcial → PARCIAL
            cobro.setMontoPagado(cobro.getMontoPagado().add(montoPago));
            cobro.setEstado(EstadoCobro.PARCIAL);
        }

        cobroRepo.save(cobro);
        return toResponse(pago);
    }

    // ─── Rechazo (admin) ───────────────────────────────────────────

    @Transactional
    public PagoResponse rechazar(Long id, RechazarPagoRequest req, Long adminId) {
        Pago pago = obtenerYValidar(id);
        pago.setEstado(EstadoPago.RECHAZADO);
        pago.setVerificadoPor(adminId);
        pago.setFechaVerificacion(LocalDateTime.now());
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
