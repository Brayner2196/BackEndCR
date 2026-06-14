package com.backendcr.residentialcomplex.dto.pqr;

import com.backendcr.residentialcomplex.entity.PQRHistorial;
import com.backendcr.residentialcomplex.entity.enums.EstadoPQR;

import java.time.Instant;

public record PQRHistorialResponse(
        Long id,
        Long pqrId,
        EstadoPQR estadoAnterior,
        EstadoPQR estadoNuevo,
        Long cambiadoPor,
        String cambiadoPorNombre,
        String cambiadoPorRol,
        String comentario,
        Instant fechaCambio
) {
    public static PQRHistorialResponse from(PQRHistorial h, String nombre, String rol) {
        return new PQRHistorialResponse(
                h.getId(),
                h.getPqrId(),
                h.getEstadoAnterior(),
                h.getEstadoNuevo(),
                h.getCambiadoPor(),
                nombre,
                rol,
                h.getComentario(),
                h.getFechaCambio()
        );
    }
}
