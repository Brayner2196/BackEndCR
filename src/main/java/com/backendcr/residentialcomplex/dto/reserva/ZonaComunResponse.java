package com.backendcr.residentialcomplex.dto.reserva;

import com.backendcr.residentialcomplex.entity.ZonaComun;

public record ZonaComunResponse(
        Long id,
        String nombre,
        String descripcion,
        Integer capacidad,
        boolean activa
) {
    public static ZonaComunResponse from(ZonaComun z) {
        return new ZonaComunResponse(z.getId(), z.getNombre(), z.getDescripcion(),
                z.getCapacidad(), z.isActiva());
    }
}
