package com.backendcr.residentialcomplex.dto.pqr;

import java.time.format.DateTimeFormatter;

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
        String respuestaAdmin,
        Long respondidoPor,
        String fechaRespuesta,
        String creadoEn
) {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
	
    public static PQRResponse from(PQR p, String residenteNombre) {
        return new PQRResponse(
                p.getId(), p.getTipo(), p.getAsunto(), p.getDescripcion(),
                p.getEstado(), p.getResidenteId(), residenteNombre,
                p.getPropiedadId(), p.getRespuestaAdmin(), p.getRespondidoPor(),
                p.getFechaRespuesta() != null ? p.getFechaRespuesta().format(FORMATTER) : null,
                p.getCreadoEn() != null ? p.getCreadoEn().format(FORMATTER) : null
        );
    }
}
