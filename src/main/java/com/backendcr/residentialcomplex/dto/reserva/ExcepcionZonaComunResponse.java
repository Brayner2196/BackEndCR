package com.backendcr.residentialcomplex.dto.reserva;

import com.backendcr.residentialcomplex.entity.ExcepcionZonaComun;
import com.backendcr.residentialcomplex.entity.enums.TipoExcepcionZona;

import java.time.LocalDate;
import java.time.LocalTime;

public record ExcepcionZonaComunResponse(
        Long id,
        Long zonaComunId,
        LocalDate fecha,
        TipoExcepcionZona tipo,
        LocalTime horaApertura,
        LocalTime horaCierre,
        String motivo
) {
    public static ExcepcionZonaComunResponse from(ExcepcionZonaComun e) {
        return new ExcepcionZonaComunResponse(
                e.getId(), e.getZonaComunId(), e.getFecha(),
                e.getTipo(), e.getHoraApertura(), e.getHoraCierre(), e.getMotivo()
        );
    }
}
