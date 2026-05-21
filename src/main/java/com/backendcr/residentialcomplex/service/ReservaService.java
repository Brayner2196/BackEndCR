package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.reserva.ReservaDecisionRequest;
import com.backendcr.residentialcomplex.dto.reserva.ReservaRequest;
import com.backendcr.residentialcomplex.dto.reserva.ReservaResponse;
import com.backendcr.residentialcomplex.entity.ExcepcionZonaComun;
import com.backendcr.residentialcomplex.entity.Reserva;
import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.entity.ZonaComun;
import com.backendcr.residentialcomplex.entity.enums.EstadoReserva;
import com.backendcr.residentialcomplex.entity.enums.TipoExcepcionZona;
import com.backendcr.residentialcomplex.repository.ExcepcionZonaComunRepository;
import com.backendcr.residentialcomplex.repository.ReservaRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.repository.ZonaComunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.config.ColombiaTimeZone;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepo;
    private final ZonaComunRepository zonaRepo;
    private final UsuarioRepository usuarioRepo;
    private final ExcepcionZonaComunRepository excepcionRepo;

    // ─── Consultas ─────────────────────────────────────────────

    public List<ReservaResponse> listarTodas() {
        return reservaRepo.findAll().stream().map(this::toResponse).toList();
    }

    public List<ReservaResponse> listarPorEstado(EstadoReserva estado) {
        return reservaRepo.findAllByEstado(estado).stream().map(this::toResponse).toList();
    }

    public List<ReservaResponse> listarPorResidente(Long residenteId) {
        return reservaRepo.findAllByResidenteId(residenteId).stream().map(this::toResponse).toList();
    }

    // ─── Crear (con validaciones completas) ───────────────────

    @Transactional
    public ReservaResponse crear(ReservaRequest req, Long residenteId) {
        ZonaComun zona = zonaRepo.findById(req.zonaComunId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Zona común no encontrada"));

        // 1. Zona activa y no suspendida
        if (!zona.isActiva()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La zona común no está disponible");
        }
        if (zona.isSuspendida()) {
            String motivo = zona.getMotivoSuspension() != null
                    ? ": " + zona.getMotivoSuspension() : "";
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La zona está suspendida temporalmente" + motivo);
        }

        // 2. Hora fin posterior a hora inicio
        if (!req.horaFin().isAfter(req.horaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "hora_fin debe ser posterior a hora_inicio");
        }

        // 3. Verificar excepción del día
        Optional<ExcepcionZonaComun> excepcionOpt =
                excepcionRepo.findByZonaComunIdAndFecha(zona.getId(), req.fecha());

        if (excepcionOpt.isPresent()) {
            ExcepcionZonaComun exc = excepcionOpt.get();
            if (exc.getTipo() == TipoExcepcionZona.CIERRE_ESPECIAL) {
                String motivo = exc.getMotivo() != null ? ": " + exc.getMotivo() : "";
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La zona está cerrada ese día" + motivo);
            }
            // APERTURA_ESPECIAL → validar dentro del horario de excepción
            if (exc.getHoraApertura() != null && exc.getHoraCierre() != null) {
                validarDentroHorario(req.horaInicio(), req.horaFin(),
                        exc.getHoraApertura(), exc.getHoraCierre(),
                        "Fuera del horario de apertura especial (" +
                                exc.getHoraApertura() + "–" + exc.getHoraCierre() + ")");
            }
        } else {
            // 4. Verificar día disponible
            validarDiaDisponible(zona, req.fecha());

            // 5. Verificar horario estándar
            if (zona.getHoraApertura() != null && zona.getHoraCierre() != null) {
                validarDentroHorario(req.horaInicio(), req.horaFin(),
                        zona.getHoraApertura(), zona.getHoraCierre(),
                        "Fuera del horario estándar (" +
                                zona.getHoraApertura() + "–" + zona.getHoraCierre() + ")");
            }
        }

        // 6. Validar duración
        long durMin = ChronoUnit.MINUTES.between(req.horaInicio(), req.horaFin());
        if (zona.getDuracionMinMinutos() != null && durMin < zona.getDuracionMinMinutos()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La reserva debe durar al menos " + zona.getDuracionMinMinutos() + " minutos");
        }
        if (zona.getDuracionMaxMinutos() != null && durMin > zona.getDuracionMaxMinutos()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La reserva no puede superar " + zona.getDuracionMaxMinutos() + " minutos");
        }

        // 7. Validar anticipación
        long diasAnticipacion = ChronoUnit.DAYS.between(ColombiaTimeZone.hoy(), req.fecha());
        if (diasAnticipacion < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se pueden crear reservas para fechas pasadas");
        }
        if (zona.getAnticipacionMinDias() != null && diasAnticipacion < zona.getAnticipacionMinDias()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se requiere reservar con al menos " + zona.getAnticipacionMinDias() + " días de anticipación");
        }
        if (zona.getAnticipacionMaxDias() != null && diasAnticipacion > zona.getAnticipacionMaxDias()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede reservar con más de " + zona.getAnticipacionMaxDias() + " días de anticipación");
        }

        // 8. Aforo: reservas activas solapadas < capacidad
        if (zona.getCapacidad() > 0) {
            long ocupadas = reservaRepo.countSolapamientos(
                    zona.getId(), req.fecha(), req.horaInicio(), req.horaFin());
            if (ocupadas >= zona.getCapacidad()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El aforo máximo (" + zona.getCapacidad() + ") ya está cubierto para ese horario");
            }
        }

        // 9. Crear reserva
        Reserva r = new Reserva();
        r.setZonaComunId(req.zonaComunId());
        r.setResidenteId(residenteId);
        r.setPropiedadId(req.propiedadId());
        r.setFecha(req.fecha());
        r.setHoraInicio(req.horaInicio());
        r.setHoraFin(req.horaFin());
        r.setObservaciones(req.observaciones());
        // Auto-aprobar si la zona no requiere aprobación manual
        r.setEstado(zona.isRequiereAprobacion() ? EstadoReserva.PENDIENTE : EstadoReserva.APROBADA);
        return toResponse(reservaRepo.save(r));
    }

    // ─── Aprobar / Rechazar / Cancelar ────────────────────────

    @Transactional
    public ReservaResponse aprobar(Long id, ReservaDecisionRequest req, Long adminId) {
        Reserva r = obtener(id);
        validarPendiente(r);
        r.setEstado(EstadoReserva.APROBADA);
        r.setDecididoPor(adminId);
        r.setMotivoDecision(req != null ? req.motivo() : null);
        r.setFechaDecision(ColombiaTimeZone.ahora());
        return toResponse(reservaRepo.save(r));
    }

    @Transactional
    public ReservaResponse rechazar(Long id, ReservaDecisionRequest req, Long adminId) {
        Reserva r = obtener(id);
        validarPendiente(r);
        r.setEstado(EstadoReserva.RECHAZADA);
        r.setDecididoPor(adminId);
        r.setMotivoDecision(req != null ? req.motivo() : null);
        r.setFechaDecision(ColombiaTimeZone.ahora());
        return toResponse(reservaRepo.save(r));
    }

    @Transactional
    public ReservaResponse cancelar(Long id, Long residenteId) {
        Reserva r = obtener(id);
        if (!r.getResidenteId().equals(residenteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No puedes cancelar reservas de otro residente");
        }
        if (r.getEstado() == EstadoReserva.RECHAZADA || r.getEstado() == EstadoReserva.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La reserva ya está " + r.getEstado());
        }
        r.setEstado(EstadoReserva.CANCELADA);
        r.setFechaDecision(ColombiaTimeZone.ahora());
        return toResponse(reservaRepo.save(r));
    }

    // ─── Helpers de validación ─────────────────────────────────

    private void validarDiaDisponible(ZonaComun zona, LocalDate fecha) {
        String csv = zona.getDiasDisponibles();
        if (csv == null || csv.isBlank()) return; // sin restricción

        String diaEspanol = diaEnEspanol(fecha.getDayOfWeek());
        boolean disponible = Arrays.stream(csv.split(","))
                .map(String::trim)
                .anyMatch(d -> d.equalsIgnoreCase(diaEspanol));
        if (!disponible) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La zona no está disponible los " + diaEspanol.toLowerCase());
        }
    }

    private void validarDentroHorario(LocalTime inicio, LocalTime fin,
                                       LocalTime apertura, LocalTime cierre,
                                       String mensaje) {
        if (inicio.isBefore(apertura) || fin.isAfter(cierre)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
        }
    }

    private static final Map<DayOfWeek, String> DIAS_ESP = Map.of(
            DayOfWeek.MONDAY,    "LUNES",
            DayOfWeek.TUESDAY,   "MARTES",
            DayOfWeek.WEDNESDAY, "MIERCOLES",
            DayOfWeek.THURSDAY,  "JUEVES",
            DayOfWeek.FRIDAY,    "VIERNES",
            DayOfWeek.SATURDAY,  "SABADO",
            DayOfWeek.SUNDAY,    "DOMINGO"
    );

    private String diaEnEspanol(DayOfWeek dow) {
        return DIAS_ESP.getOrDefault(dow, dow.name());
    }

    private void validarPendiente(Reserva r) {
        if (r.getEstado() != EstadoReserva.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden decidir reservas en estado PENDIENTE");
        }
    }

    private Reserva obtener(Long id) {
        return reservaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Reserva no encontrada"));
    }

    private ReservaResponse toResponse(Reserva r) {
        String zonaNombre = zonaRepo.findById(r.getZonaComunId())
                .map(ZonaComun::getNombre).orElse("N/A");
        String residenteNombre = usuarioRepo.findById(r.getResidenteId())
                .map(Usuario::getNombre).orElse("N/A");
        return ReservaResponse.from(r, zonaNombre, residenteNombre);
    }
}
