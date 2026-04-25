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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepo;
    private final CobroRepository cobroRepo;
    private final UsuarioRepository usuarioRepo;

    public List<PagoResponse> listarPorEstado(EstadoPago estado) {
        return pagoRepo.findAllByEstado(estado).stream().map(this::toResponse).toList();
    }

    public List<PagoResponse> listarPorUsuario(Long usuarioId) {
        return pagoRepo.findAllByUsuarioId(usuarioId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public PagoResponse registrar(PagoRequest req, Long usuarioId) {
        Cobro cobro = cobroRepo.findById(req.cobroId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cobro no encontrado"));
        if (cobro.getEstado() == EstadoCobro.PAGADO || cobro.getEstado() == EstadoCobro.EXONERADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este cobro ya está " + cobro.getEstado());
        }
        if (pagoRepo.findByCobroIdAndEstado(req.cobroId(), EstadoPago.PENDIENTE_VERIFICACION).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un pago pendiente de verificación para este cobro");
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
        cobro.setEstado(EstadoCobro.PAGADO);
        cobroRepo.save(cobro);
        return toResponse(pago);
    }

    @Transactional
    public PagoResponse rechazar(Long id, RechazarPagoRequest req, Long adminId) {
        Pago pago = obtenerYValidar(id);
        pago.setEstado(EstadoPago.RECHAZADO);
        pago.setVerificadoPor(adminId);
        pago.setFechaVerificacion(LocalDateTime.now());
        pago.setMotivoRechazo(req.motivoRechazo());
        return toResponse(pagoRepo.save(pago));
    }

    private Pago obtenerYValidar(Long id) {
        Pago pago = pagoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
        if (pago.getEstado() != EstadoPago.PENDIENTE_VERIFICACION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden procesar pagos en estado PENDIENTE_VERIFICACION");
        }
        return pago;
    }

    private PagoResponse toResponse(Pago p) {
        String nombre = usuarioRepo.findById(p.getUsuarioId())
                .map(Usuario::getNombre).orElse("N/A");
        return PagoResponse.from(p, nombre);
    }
}
