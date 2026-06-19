package com.backendcr.residentialcomplex.dto.pqr;

import java.time.Instant;

import com.backendcr.residentialcomplex.entity.PQR;
import com.backendcr.residentialcomplex.entity.enums.EstadoPQR;
import com.backendcr.residentialcomplex.entity.enums.TipoPQR;

public record PQRResponse(
        Long id,
        TipoPQR tipo,
        String asunto,
        String descripcion,
        EstadoPQR estado,
        Long residenteId,
        String residenteNombre,
        Long propiedadId,
        String propiedadIdentificador,
        String respuestaAdmin,
        Long respondidoPor,
        Instant fechaRespuesta,
        Instant creadoEn
) {
    public static PQRResponse from(PQR p, String residenteNombre, String propiedadIdentificador) {
        return new PQRResponse(
                p.getId(), p.getTipo(), p.getAsunto(), p.getDescripcion(),
                p.getEstado(), p.getResidenteId(), residenteNombre,
                p.getPropiedadId(), propiedadIdentificador,
                p.getRespuestaAdmin(), p.getRespondidoPor(),
                p.getFechaRespuesta(),
                p.getCreadoEn()
        );
    }
}
