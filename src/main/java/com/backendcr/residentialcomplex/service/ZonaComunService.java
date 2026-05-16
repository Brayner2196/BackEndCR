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

import java.time.LocalDate;
import java.time.LocalTime;
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

        z.setHoraApertura(parseTime(req.horaApertura()));
        z.setHoraCierre(parseTime(req.horaCierre()));
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

        LocalTime apertura = parseTime(req.horaApertura());
        LocalTime cierre   = parseTime(req.horaCierre());
        LocalDate fecha    = parseDate(req.fecha());

        // Para APERTURA_ESPECIAL se requieren horas
        if (req.tipo() == TipoExcepcionZona.APERTURA_ESPECIAL) {
            if (apertura == null || cierre == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "APERTURA_ESPECIAL requiere horaApertura y horaCierre");
            }
            if (!cierre.isAfter(apertura)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "horaCierre debe ser posterior a horaApertura");
            }
        }

        // No se puede tener dos excepciones el mismo día
        excepcionRepo.findByZonaComunIdAndFecha(zonaId, fecha).ifPresent(e -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una excepción para la fecha " + fecha);
        });

        ExcepcionZonaComun ex = new ExcepcionZonaComun();
        ex.setZonaComunId(zonaId);
        ex.setFecha(fecha);
        ex.setTipo(req.tipo());
        ex.setHoraApertura(apertura);
        ex.setHoraCierre(cierre);
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

    // ─── Helpers ───────────────────────────────────────────────

    public ZonaComun obtener(Long id) {
        return zonaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Zona común no encontrada"));
    }

    /**
     * Parsea "HH:mm", "HH:mm:ss" o "HH:mm:ss+00" → LocalTime.
     * Retorna null si la cadena es nula o vacía.
     */
    private LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            // Tomar solo los primeros 8 caracteres para ignorar offset de timezone
            String normalized = s.length() > 8 ? s.substring(0, 8) : s;
            // Si viene "HH:mm" (5 chars), agregarle ":00"
            if (normalized.length() == 5) normalized += ":00";
            return LocalTime.parse(normalized);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato de hora inválido: " + s + ". Use HH:mm o HH:mm:ss");
        }
    }

    /**
     * Parsea "yyyy-MM-dd" o "yyyy-MM-ddT..." → LocalDate.
     * Retorna null si la cadena es nula o vacía.
     */
    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.substring(0, 10));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato de fecha inválido: " + s + ". Use yyyy-MM-dd");
        }
    }
}
