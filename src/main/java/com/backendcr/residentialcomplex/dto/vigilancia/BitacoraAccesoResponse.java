package com.backendcr.residentialcomplex.dto.vigilancia;

import com.backendcr.residentialcomplex.entity.BitacoraAcceso;
import com.backendcr.residentialcomplex.entity.enums.ResultadoAcceso;
import com.backendcr.residentialcomplex.entity.enums.TipoEventoAcceso;

import java.time.Instant;

public record BitacoraAccesoResponse(
        Long id,
        TipoEventoAcceso tipoEvento,
        ResultadoAcceso resultado,
        String descripcion,
        Long propiedadId,
        String propiedadIdentificador,
        String placa,
        String documento,
        String nombreVisitante,
        Long vigilanteId,
        Instant creadoEn
) {
    public static BitacoraAccesoResponse from(BitacoraAcceso b, String propiedadIdentificador) {
        return new BitacoraAccesoResponse(
                b.getId(), b.getTipoEvento(), b.getResultado(), b.getDescripcion(),
                b.getPropiedadId(), propiedadIdentificador, b.getPlaca(), b.getDocumento(),
                b.getNombreVisitante(), b.getVigilanteId(), b.getCreadoEn());
    }
}
