package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.reserva.ReservaDecisionRequest;
import com.backendcr.residentialcomplex.dto.reserva.ReservaRequest;
import com.backendcr.residentialcomplex.dto.reserva.ReservaResponse;
import com.backendcr.residentialcomplex.entity.Reserva;
import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.entity.ZonaComun;
import com.backendcr.residentialcomplex.entity.enums.EstadoReserva;
import com.backendcr.residentialcomplex.repository.ReservaRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.repository.ZonaComunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepo;
    private final ZonaComunRepository zonaRepo;
    private final UsuarioRepository usuarioRepo;

    public List<ReservaResponse> listarTodas() {
        return reservaRepo.findAll().stream().map(this::toResponse).toList();
    }

    public List<ReservaResponse> listarPorEstado(EstadoReserva estado) {
        return reservaRepo.findAllByEstado(estado).stream().map(this::toResponse).toList();
    }

    public List<ReservaResponse> listarPorResidente(Long residenteId) {
        return reservaRepo.findAllByResidenteId(residenteId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReservaResponse crear(ReservaRequest req, Long residenteId) {
        ZonaComun zona = zonaRepo.findById(req.zonaComunId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zona común no encontrada"));
        if (!zona.isActiva()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La zona común no está disponible");
        }
        if (!req.horaFin().isAfter(req.horaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hora_fin debe ser posterior a hora_inicio");
        }
        Reserva r = new Reserva();
        r.setZonaComunId(req.zonaComunId());
        r.setResidenteId(residenteId);
        r.setPropiedadId(req.propiedadId());
        r.setFecha(req.fecha());
        r.setHoraInicio(req.horaInicio());
        r.setHoraFin(req.horaFin());
        r.setObservaciones(req.observaciones());
        r.setEstado(EstadoReserva.PENDIENTE);
        return toResponse(reservaRepo.save(r));
    }

    @Transactional
    public ReservaResponse aprobar(Long id, ReservaDecisionRequest req, Long adminId) {
        Reserva r = obtener(id);
        validarPendiente(r);
        r.setEstado(EstadoReserva.APROBADA);
        r.setDecididoPor(adminId);
        r.setMotivoDecision(req != null ? req.motivo() : null);
        r.setFechaDecision(LocalDateTime.now());
        return toResponse(reservaRepo.save(r));
    }

    @Transactional
    public ReservaResponse rechazar(Long id, ReservaDecisionRequest req, Long adminId) {
        Reserva r = obtener(id);
        validarPendiente(r);
        r.setEstado(EstadoReserva.RECHAZADA);
        r.setDecididoPor(adminId);
        r.setMotivoDecision(req != null ? req.motivo() : null);
        r.setFechaDecision(LocalDateTime.now());
        return toResponse(reservaRepo.save(r));
    }

    @Transactional
    public ReservaResponse cancelar(Long id, Long residenteId) {
        Reserva r = obtener(id);
        if (!r.getResidenteId().equals(residenteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes cancelar reservas de otro residente");
        }
        if (r.getEstado() == EstadoReserva.RECHAZADA || r.getEstado() == EstadoReserva.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva ya está " + r.getEstado());
        }
        r.setEstado(EstadoReserva.CANCELADA);
        r.setFechaDecision(LocalDateTime.now());
        return toResponse(reservaRepo.save(r));
    }

    private void validarPendiente(Reserva r) {
        if (r.getEstado() != EstadoReserva.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden decidir reservas en estado PENDIENTE");
        }
    }

    private Reserva obtener(Long id) {
        return reservaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
    }

    private ReservaResponse toResponse(Reserva r) {
        String zonaNombre = zonaRepo.findById(r.getZonaComunId())
                .map(ZonaComun::getNombre).orElse("N/A");
        String residenteNombre = usuarioRepo.findById(r.getResidenteId())
                .map(Usuario::getNombre).orElse("N/A");
        return ReservaResponse.from(r, zonaNombre, residenteNombre);
    }
}
