package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.pago.*;
import com.backendcr.residentialcomplex.entity.*;
import com.backendcr.residentialcomplex.entity.enums.EstadoCobro;
import com.backendcr.residentialcomplex.entity.enums.EstadoPago;
import com.backendcr.residentialcomplex.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.config.TenantClock;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AbonoService {

    private final AbonoRepository abonoRepo;
    private final CobroRepository cobroRepo;
    private final MovimientoAbonoRepository movimientoRepo;
    private final SaldoFavorRepository saldoFavorRepo;
    private final UsuarioRepository usuarioRepo;

    private static final List<EstadoCobro> ESTADOS_DEUDORES =
            List.of(EstadoCobro.PENDIENTE, EstadoCobro.PARCIAL, EstadoCobro.VENCIDO);

    // ─── Registro (residente) ───────────────────────────────────────

    @Transactional
    public AbonoResponse registrar(AbonoRequest req, Long usuarioId) {
        Abono abono = new Abono();
        abono.setPropiedadId(req.propiedadId());
        abono.setUsuarioId(usuarioId);
        abono.setMontoTotal(req.montoTotal());
        abono.setFechaPago(req.fechaPago());
        abono.setMetodoPago(req.metodoPago());
        abono.setReferencia(req.referencia());
        abono.setUrlComprobante(req.urlComprobante());
        abono.setNotas(req.notas());
        abono.setEstado(EstadoPago.PENDIENTE_VERIFICACION);
        Abono guardado = abonoRepo.save(abono);
        return toResponse(guardado, List.of());
    }

    // ─── Verificación (admin) — aquí corre el FIFO ─────────────────

    @Transactional
    public AbonoResponse verificar(Long id, VerificarPagoRequest req, Long adminId) {
        Abono abono = obtenerYValidar(id);
        abono.setEstado(EstadoPago.VERIFICADO);
        abono.setVerificadoPor(adminId);
        abono.setFechaVerificacion(TenantClock.ahora());
        if (req.notas() != null) abono.setNotas(req.notas());
        abonoRepo.save(abono);

        List<MovimientoAbono> movimientos = distribuirFIFO(abono);
        return toResponse(abono, movimientos.stream().map(MovimientoAbonoDto::from).toList());
    }

    @Transactional
    public AbonoResponse rechazar(Long id, RechazarPagoRequest req, Long adminId) {
        Abono abono = obtenerYValidar(id);
        abono.setEstado(EstadoPago.RECHAZADO);
        abono.setVerificadoPor(adminId);
        abono.setFechaVerificacion(TenantClock.ahora());
        abono.setMotivoRechazo(req.motivoRechazo());
        return toResponse(abonoRepo.save(abono), List.of());
    }

    // ─── Consultas ─────────────────────────────────────────────────

    public List<AbonoResponse> listarPorEstado(EstadoPago estado) {
        return abonoRepo.findAllByEstado(estado).stream()
                .map(a -> toResponse(a, movimientosPara(a.getId())))
                .toList();
    }

    public List<AbonoResponse> listarPorUsuario(Long usuarioId) {
        return abonoRepo.findAllByUsuarioId(usuarioId).stream()
                .map(a -> toResponse(a, movimientosPara(a.getId())))
                .toList();
    }

    // ─── Simulación (preview sin guardar) ─────────────────────────

    public SimularAbonoResponse simular(Long propiedadId, BigDecimal montoAbono) {
        SaldoFavor sf = saldoFavorRepo.findByPropiedadId(propiedadId).orElse(null);
        BigDecimal saldoPrevio = sf != null ? sf.getSaldo() : BigDecimal.ZERO;
        BigDecimal disponible = saldoPrevio.add(montoAbono);

        List<Cobro> cobros = cobroRepo.findAllByPropiedadIdAndEstadoInOrderByFechaGeneracionAsc(
                propiedadId, ESTADOS_DEUDORES);

        List<MovimientoAbonoDto> distribucion = new ArrayList<>();
        BigDecimal restante = disponible;

        for (Cobro cobro : cobros) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal pendiente = cobro.getMontoPendiente();
            BigDecimal aplicar = restante.min(pendiente);
            distribucion.add(new MovimientoAbonoDto(
                    cobro.getId(), aplicar,
                    "Cobro #" + cobro.getId() + " — " + cobro.getConcepto()));
            restante = restante.subtract(aplicar);
        }

        if (restante.compareTo(BigDecimal.ZERO) > 0) {
            distribucion.add(new MovimientoAbonoDto(null, restante, "Saldo a favor"));
        }

        return new SimularAbonoResponse(montoAbono, saldoPrevio, disponible, distribucion, restante);
    }

    public SaldoFavorResponse consultarSaldoFavor(Long propiedadId) {
        return saldoFavorRepo.findByPropiedadId(propiedadId)
                .map(SaldoFavorResponse::from)
                .orElse(SaldoFavorResponse.sinSaldo(propiedadId));
    }

    // ─── Lógica FIFO ───────────────────────────────────────────────

    /**
     * Distribuye el abono entre los cobros de la propiedad del más antiguo al más nuevo.
     * Si hay saldo a favor previo, se suma al monto del abono antes de distribuir.
     * El remanente (si queda tras saldar todo) se acumula en SaldoFavor.
     */
    private List<MovimientoAbono> distribuirFIFO(Abono abono) {
        Long propiedadId = abono.getPropiedadId();

        // 1. Obtener y resetear saldo a favor previo
        SaldoFavor saldoFavor = saldoFavorRepo.findByPropiedadId(propiedadId)
                .orElseGet(() -> crearSaldoFavor(propiedadId, abono.getUsuarioId()));
        BigDecimal disponible = saldoFavor.getSaldo().add(abono.getMontoTotal());
        saldoFavor.setSaldo(BigDecimal.ZERO);

        // 2. Obtener cobros deudores ordenados FIFO
        List<Cobro> cobros = cobroRepo.findAllByPropiedadIdAndEstadoInOrderByFechaGeneracionAsc(
                propiedadId, ESTADOS_DEUDORES);

        // 3. Distribuir
        List<MovimientoAbono> movimientos = new ArrayList<>();
        for (Cobro cobro : cobros) {
            if (disponible.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal pendiente = cobro.getMontoPendiente();
            BigDecimal aplicar = disponible.min(pendiente);

            cobro.setMontoPagado(cobro.getMontoPagado().add(aplicar));
            cobro.setEstado(cobro.getMontoPagado().compareTo(cobro.getMontoTotal()) >= 0
                    ? EstadoCobro.PAGADO
                    : EstadoCobro.PARCIAL);
            cobroRepo.save(cobro);

            MovimientoAbono mov = new MovimientoAbono();
            mov.setAbonoId(abono.getId());
            mov.setCobroId(cobro.getId());
            mov.setMontoAplicado(aplicar);
            mov.setDescripcion("Cobro #" + cobro.getId() + " — " + cobro.getConcepto());
            movimientos.add(movimientoRepo.save(mov));

            disponible = disponible.subtract(aplicar);
        }

        // 4. Remanente → saldo a favor
        if (disponible.compareTo(BigDecimal.ZERO) > 0) {
            saldoFavor.setSaldo(disponible);

            MovimientoAbono movSaldo = new MovimientoAbono();
            movSaldo.setAbonoId(abono.getId());
            movSaldo.setCobroId(null);
            movSaldo.setMontoAplicado(disponible);
            movSaldo.setDescripcion("Saldo a favor acumulado");
            movimientos.add(movimientoRepo.save(movSaldo));
        }

        saldoFavorRepo.save(saldoFavor);
        return movimientos;
    }

    private SaldoFavor crearSaldoFavor(Long propiedadId, Long usuarioId) {
        SaldoFavor sf = new SaldoFavor();
        sf.setPropiedadId(propiedadId);
        sf.setUsuarioId(usuarioId);
        sf.setSaldo(BigDecimal.ZERO);
        return saldoFavorRepo.save(sf);
    }

    // ─── Helpers ───────────────────────────────────────────────────

    private Abono obtenerYValidar(Long id) {
        Abono abono = abonoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Abono no encontrado"));
        if (abono.getEstado() != EstadoPago.PENDIENTE_VERIFICACION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden procesar abonos en estado PENDIENTE_VERIFICACION");
        }
        return abono;
    }

    private List<MovimientoAbonoDto> movimientosPara(Long abonoId) {
        return movimientoRepo.findAllByAbonoId(abonoId)
                .stream().map(MovimientoAbonoDto::from).toList();
    }

    private AbonoResponse toResponse(Abono a, List<MovimientoAbonoDto> movimientos) {
        String nombre = usuarioRepo.findById(a.getUsuarioId())
                .map(Usuario::getNombre).orElse("N/A");
        return AbonoResponse.from(a, nombre, movimientos);
    }
}
