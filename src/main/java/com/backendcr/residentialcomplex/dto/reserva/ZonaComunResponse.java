package com.backendcr.residentialcomplex.dto.reserva;

import com.backendcr.residentialcomplex.entity.ZonaComun;

import java.time.LocalTime;

public record ZonaComunResponse(
        Long id,
        String nombre,
        String descripcion,
        Integer capacidad,
        boolean activa,

        // Horario estándar
        LocalTime horaApertura,
        LocalTime horaCierre,
        String diasDisponibles,

        // Reglas de duración
        Integer duracionMinMinutos,
        Integer duracionMaxMinutos,

        // Reglas de anticipación
        Integer anticipacionMinDias,
        Integer anticipacionMaxDias,

        boolean requiereAprobacion,

        // Suspensión
        boolean suspendida,
        String motivoSuspension
) {
    public static ZonaComunResponse from(ZonaComun z) {
        return new ZonaComunResponse(
                z.getId(), z.getNombre(), z.getDescripcion(),
                z.getCapacidad(), z.isActiva(),
                z.getHoraApertura(), z.getHoraCierre(), z.getDiasDisponibles(),
                z.getDuracionMinMinutos(), z.getDuracionMaxMinutos(),
                z.getAnticipacionMinDias(), z.getAnticipacionMaxDias(),
                z.isRequiereAprobacion(),
                z.isSuspendida(), z.getMotivoSuspension()
        );
    }
}
