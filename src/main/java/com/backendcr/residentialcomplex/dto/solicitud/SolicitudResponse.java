package com.backendcr.residentialcomplex.dto.solicitud;

import com.backendcr.residentialcomplex.entity.Solicitud;
import com.backendcr.residentialcomplex.entity.enums.EstadoSolicitud;
import com.backendcr.residentialcomplex.entity.enums.TipoSolicitud;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SolicitudResponse(
        Long id,
        Long publicacionId,
        String publicacionTitulo,
        BigDecimal publicacionPrecio,
        Long compradorId,
        String compradorNombre,
        Long vendedorId,
        String vendedorNombre,
        TipoSolicitud tipo,
        int cantidad,
        String notas,
        EstadoSolicitud estado,
        LocalDateTime creadoEn
) {
    public static SolicitudResponse from(Solicitud s) {
        return new SolicitudResponse(
                s.getId(),
                s.getPublicacionId(),
                s.getPublicacionTitulo(),
                s.getPublicacionPrecio(),
                s.getCompradorId(),
                s.getCompradorNombre(),
                s.getVendedorId(),
                s.getVendedorNombre(),
                s.getTipo(),
                s.getCantidad(),
                s.getNotas(),
                s.getEstado(),
                s.getCreadoEn()
        );
    }
}
