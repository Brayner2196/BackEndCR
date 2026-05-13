package com.backendcr.residentialcomplex.dto.pqr;

import com.backendcr.residentialcomplex.entity.PQRHistorial;
import com.backendcr.residentialcomplex.entity.enums.EstadoPQR;

import java.time.format.DateTimeFormatter;

public record PQRHistorialResponse(
        Long id,
        Long pqrId,
        EstadoPQR estadoAnterior,
        EstadoPQR estadoNuevo,
        Long cambiadoPor,
        String comentario,
        String fechaCambio
) {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public static PQRHistorialResponse from(PQRHistorial h) {
        return new PQRHistorialResponse(
                h.getId(),
                h.getPqrId(),
                h.getEstadoAnterior(),
                h.getEstadoNuevo(),
                h.getCambiadoPor(),
                h.getComentario(),
                h.getFechaCambio() != null ? h.getFechaCambio().format(FORMATTER) : null
        );
    }
}
