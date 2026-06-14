package com.backendcr.residentialcomplex.service.reserva;

import com.backendcr.residentialcomplex.config.TenantClock;
import com.backendcr.residentialcomplex.dto.cartera.ResultadoRestriccion;
import com.backendcr.residentialcomplex.entity.Cobro;
import com.backendcr.residentialcomplex.entity.Propiedad;
import com.backendcr.residentialcomplex.entity.Reserva;
import com.backendcr.residentialcomplex.entity.UsuarioPropiedad;
import com.backendcr.residentialcomplex.entity.ZonaComun;
import com.backendcr.residentialcomplex.entity.enums.AccionRestringible;
import com.backendcr.residentialcomplex.entity.enums.EstadoCobro;
import com.backendcr.residentialcomplex.entity.enums.RolPropiedad;
import com.backendcr.residentialcomplex.repository.CobroRepository;
import com.backendcr.residentialcomplex.repository.PropiedadRepository;
import com.backendcr.residentialcomplex.repository.ReservaRepository;
import com.backendcr.residentialcomplex.repository.UsuarioPropiedadRepository;
import com.backendcr.residentialcomplex.service.cartera.RestriccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Validaciones de negocio de las reservas, separadas del {@link com.backendcr.residentialcomplex.service.ReservaService}
 * para que cada regla sea individualmente testeable y reutilizable.
 *
 * Cada método valida un parámetro de la {@link ZonaComun} que antes se guardaba
 * pero nunca se aplicaba (cuotas, restricciones de acceso, ventana de cancelación).
 */
@Component
@RequiredArgsConstructor
public class ReglasReservaValidator {

    private final ReservaRepository reservaRepo;
    private final UsuarioPropiedadRepository usuarioPropiedadRepo;
    private final CobroRepository cobroRepo;
    private final PropiedadRepository propiedadRepo;
    private final RestriccionService restriccionService;

    // ── Cuota por residente (max_reservas_semana / max_reservas_mes) ──────────

    /**
     * Verifica que el residente no supere las cuotas configuradas en la zona
     * para la semana y el mes de la fecha solicitada. Cuenta solo reservas
     * activas (PENDIENTE / APROBADA).
     */
    public void validarCuotas(ZonaComun zona, Long residenteId, LocalDate fecha) {
        if (zona.getMaxReservasSemana() != null) {
            LocalDate lunes = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate domingo = fecha.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            long enSemana = reservaRepo.countActivasResidenteZonaEnRango(
                    residenteId, zona.getId(), lunes, domingo);
            if (enSemana >= zona.getMaxReservasSemana()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Alcanzaste el máximo de " + zona.getMaxReservasSemana()
                                + " reserva(s) por semana para esta zona");
            }
        }
        if (zona.getMaxReservasMes() != null) {
            LocalDate primero = fecha.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate ultimo = fecha.with(TemporalAdjusters.lastDayOfMonth());
            long enMes = reservaRepo.countActivasResidenteZonaEnRango(
                    residenteId, zona.getId(), primero, ultimo);
            if (enMes >= zona.getMaxReservasMes()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Alcanzaste el máximo de " + zona.getMaxReservasMes()
                                + " reserva(s) por mes para esta zona");
            }
        }
    }

    // ── Restricciones de acceso (solo_propietarios / sin_deuda / solo_torre) ──

    /**
     * Valida las restricciones de acceso de la zona contra el residente y la
     * propiedad con la que reserva.
     */
    public void validarAcceso(ZonaComun zona, Long residenteId, Long propiedadId) {
        if (zona.isSoloPropietarios()) {
            boolean esPropietario = usuarioPropiedadRepo
                    .findByUsuarioIdAndPropiedadId(residenteId, propiedadId)
                    .map(up -> up.getRol() == RolPropiedad.PROPIETARIO)
                    .orElse(false);
            if (!esPropietario) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Esta zona solo puede ser reservada por propietarios");
            }
        }

        if (zona.isSinDeudaPendiente() && tieneDeudaPendiente(residenteId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No puedes reservar mientras tengas cobros pendientes o vencidos");
        }

        if (zona.getSoloTorre() != null && !zona.getSoloTorre().isBlank()
                && !propiedadEnTorre(propiedadId, zona.getSoloTorre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Esta zona está restringida a: " + zona.getSoloTorre());
        }

        // Restricción por estado de cartera configurada por el conjunto.
        // Degradación segura: si no hay configuración, no bloquea.
        ResultadoRestriccion r = restriccionService.verificar(
                propiedadId, AccionRestringible.RESERVAR_ZONA_COMUN);
        if (!r.permitido()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, r.mensaje());
        }
    }

    /** Hay deuda si existe algún cobro VENCIDO, o PENDIENTE/PARCIAL ya pasado de fecha límite. */
    private boolean tieneDeudaPendiente(Long residenteId) {
        List<Long> propiedadIds = usuarioPropiedadRepo.findByUsuarioId(residenteId)
                .stream().map(UsuarioPropiedad::getPropiedadId).toList();
        if (propiedadIds.isEmpty()) return false;
        LocalDate hoy = TenantClock.hoy();
        return cobroRepo.findAllByPropiedadIdIn(propiedadIds).stream().anyMatch(c ->
                c.getEstado() == EstadoCobro.VENCIDO
                        || ((c.getEstado() == EstadoCobro.PENDIENTE || c.getEstado() == EstadoCobro.PARCIAL)
                        && c.getFechaLimitePago() != null
                        && c.getFechaLimitePago().isBefore(hoy)));
    }

    /**
     * Verifica que la propiedad (o alguno de sus ancestros en la jerarquía)
     * coincida con el texto de torre/bloque configurado. Coincidencia laxa
     * (contains, sin distinguir mayúsculas) porque el campo es texto libre.
     */
    private boolean propiedadEnTorre(Long propiedadId, String soloTorre) {
        String objetivo = soloTorre.trim().toLowerCase();
        Long actualId = propiedadId;
        int guard = 0; // evita ciclos en datos corruptos
        while (actualId != null && guard++ < 20) {
            Propiedad p = propiedadRepo.findById(actualId).orElse(null);
            if (p == null) break;
            if (p.getIdentificador() != null
                    && p.getIdentificador().toLowerCase().contains(objetivo)) {
                return true;
            }
            actualId = p.getParentId();
        }
        return false;
    }

    // ── Ventana de cancelación (cancelacion_horas_antes) ──────────────────────

    /**
     * Valida que falten al menos {@code cancelacionHorasAntes} horas para el
     * inicio de la reserva. Si la zona no define la regla, no restringe.
     */
    public void validarVentanaCancelacion(ZonaComun zona, Reserva reserva) {
        if (zona.getCancelacionHorasAntes() == null) return;
        LocalDateTime inicio = LocalDateTime.of(reserva.getFecha(), reserva.getHoraInicio());
        long horasRestantes = ChronoUnit.HOURS.between(TenantClock.ahoraEnZona(), inicio);
        if (horasRestantes < zona.getCancelacionHorasAntes()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede cancelar con al menos "
                            + zona.getCancelacionHorasAntes() + " horas de anticipación");
        }
    }
}
