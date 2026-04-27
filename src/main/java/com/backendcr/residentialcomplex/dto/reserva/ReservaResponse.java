package com.backendcr.residentialcomplex.dto.reserva;

import com.backendcr.residentialcomplex.entity.Reserva;
import com.backendcr.residentialcomplex.entity.enums.EstadoReserva;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaResponse(
        Long id,
        Long zonaComunId,
        String zonaComunNombre,
        Long residenteId,
        String residenteNombre,
        Long propiedadId,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        EstadoReserva estado,
        String observaciones,
        String motivoDecision,
        String fechaDecision,
        String creadoEn
) {
    public static ReservaResponse from(Reserva r, String zonaNombre, String residenteNombre) {
        return new ReservaResponse(
                r.getId(), r.getZonaComunId(), zonaNombre,
                r.getResidenteId(), residenteNombre, r.getPropiedadId(),
                r.getFecha(), r.getHoraInicio(), r.getHoraFin(),
                r.getEstado(), r.getObservaciones(), r.getMotivoDecision(),
                r.getFechaDecision() != null ? r.getFechaDecision().toString() : null,
                r.getCreadoEn() != null ? r.getCreadoEn().toString() : null
        );
    }
}
