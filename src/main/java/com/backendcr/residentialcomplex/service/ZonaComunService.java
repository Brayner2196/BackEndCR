package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.reserva.*;
import com.backendcr.residentialcomplex.entity.ExcepcionZonaComun;
import com.backendcr.residentialcomplex.entity.ZonaComun;
import com.backendcr.residentialcomplex.entity.enums.TipoExcepcionZona;
import com.backendcr.residentialcomplex.repository.ExcepcionZonaComunRepository;
import com.backendcr.residentialcomplex.repository.ZonaComunRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ZonaComunService {

    private final ZonaComunRepository zonaRepo;
    private final ExcepcionZonaComunRepository excepcionRepo;

    // ─── Consultas ─────────────────────────────────────────────

    public List<ZonaComunResponse> listarTodas() {
        return zonaRepo.findAll().stream().map(ZonaComunResponse::from).toList();
    }

    /** Solo activas Y no suspendidas (lo que ve el residente). */
    public List<ZonaComunResponse> listarActivas() {
        return zonaRepo.findAll().stream()
                .filter(z -> z.isActiva() && !z.isSuspendida())
                .map(ZonaComunResponse::from)
                .toList();
    }

    // ─── Crear / Actualizar ────────────────────────────────────

    @Transactional
    public ZonaComunResponse crear(@Valid ZonaComunRequest req) {
        ZonaComun z = aplicarCampos(new ZonaComun(), req);
        return ZonaComunResponse.from(zonaRepo.save(z));
    }

    @Transactional
    public ZonaComunResponse actualizar(Long id, @Valid ZonaComunRequest req) {
        ZonaComun z = obtener(id);
        aplicarCampos(z, req);
        return ZonaComunResponse.from(zonaRepo.save(z));
    }

    private ZonaComun aplicarCampos(ZonaComun z, ZonaComunRequest req) {
        z.setNombre(req.nombre());
        z.setDescripcion(req.descripcion());
        if (req.capacidad() != null) z.setCapacidad(req.capacidad());
        if (req.activa() != null) z.setActiva(req.activa());

        z.setHoraApertura(req.horaApertura());
        z.setHoraCierre(req.horaCierre());
        z.setDiasDisponibles(req.diasDisponibles());
        z.setDuracionMinMinutos(req.duracionMinMinutos());
        z.setDuracionMaxMinutos(req.duracionMaxMinutos());
        z.setAnticipacionMinDias(req.anticipacionMinDias());
        z.setAnticipacionMaxDias(req.anticipacionMaxDias());
        if (req.requiereAprobacion() != null) z.setRequiereAprobacion(req.requiereAprobacion());
        return z;
    }

    // ─── Suspensión ────────────────────────────────────────────

    @Transactional
    public ZonaComunResponse suspender(Long id, String motivo) {
        ZonaComun z = obtener(id);
        if (z.isSuspendida()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La zona ya está suspendida");
        }
        z.setSuspendida(true);
        z.setMotivoSuspension(motivo);
        return ZonaComunResponse.from(zonaRepo.save(z));
    }

    @Transactional
    public ZonaComunResponse reactivar(Long id) {
        ZonaComun z = obtener(id);
        if (!z.isSuspendida()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La zona no está suspendida");
        }
        z.setSuspendida(false);
        z.setMotivoSuspension(null);
        return ZonaComunResponse.from(zonaRepo.save(z));
    }

    // ─── Excepciones ───────────────────────────────────────────

    public List<ExcepcionZonaComunResponse> listarExcepciones(Long zonaId) {
        obtener(zonaId); // valida existencia
        return excepcionRepo.findAllByZonaComunIdOrderByFechaAsc(zonaId)
                .stream().map(ExcepcionZonaComunResponse::from).toList();
    }

    @Transactional
    public ExcepcionZonaComunResponse agregarExcepcion(Long zonaId,
                                                        @Valid ExcepcionZonaComunRequest req) {
        obtener(zonaId); // valida existencia

        // Para APERTURA_ESPECIAL se requieren horas
        if (req.tipo() == TipoExcepcionZona.APERTURA_ESPECIAL) {
            if (req.horaApertura() == null || req.horaCierre() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "APERTURA_ESPECIAL requiere horaApertura y horaCierre");
            }
            if (!req.horaCierre().isAfter(req.horaApertura())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "horaCierre debe ser posterior a horaApertura");
            }
        }

        // No se puede tener dos excepciones el mismo día
        excepcionRepo.findByZonaComunIdAndFecha(zonaId, req.fecha()).ifPresent(e -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una excepción para la fecha " + req.fecha());
        });

        ExcepcionZonaComun ex = new ExcepcionZonaComun();
        ex.setZonaComunId(zonaId);
        ex.setFecha(req.fecha());
        ex.setTipo(req.tipo());
        ex.setHoraApertura(req.horaApertura());
        ex.setHoraCierre(req.horaCierre());
        ex.setMotivo(req.motivo());
        return ExcepcionZonaComunResponse.from(excepcionRepo.save(ex));
    }

    @Transactional
    public void eliminarExcepcion(Long zonaId, Long excepcionId) {
        obtener(zonaId);
        excepcionRepo.findById(excepcionId).ifPresent(e -> {
            if (!e.getZonaComunId().equals(zonaId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "La excepción no pertenece a esta zona");
            }
            excepcionRepo.delete(e);
        });
    }

    // ─── Helper ────────────────────────────────────────────────

    public ZonaComun obtener(Long id) {
        return zonaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Zona común no encontrada"));
    }
}
