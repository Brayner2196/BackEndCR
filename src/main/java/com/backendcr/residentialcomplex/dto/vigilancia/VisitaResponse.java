package com.backendcr.residentialcomplex.dto.vigilancia;

import com.backendcr.residentialcomplex.entity.Visita;
import com.backendcr.residentialcomplex.entity.enums.EstadoVisita;

import java.time.Instant;

public record VisitaResponse(
        Long id,
        String codigo,
        String nombreVisitante,
        String documento,
        String placa,
        String motivo,
        Long propiedadId,
        String propiedadIdentificador,
        EstadoVisita estado,
        Instant expiraEn,
        Instant ingresoEn,
        Instant creadoEn
) {
    public static VisitaResponse from(Visita v, String propiedadIdentificador) {
        return new VisitaResponse(
                v.getId(), v.getCodigo(), v.getNombreVisitante(), v.getDocumento(),
                v.getPlaca(), v.getMotivo(), v.getPropiedadId(), propiedadIdentificador,
                v.getEstado(), v.getExpiraEn(), v.getIngresoEn(), v.getCreadoEn());
    }
}
