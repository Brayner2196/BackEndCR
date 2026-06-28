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
        int cantidadPersonas,
        String acompanantes,
        Long propiedadId,
        String propiedadIdentificador,
        EstadoVisita estado,
        Instant franjaDesde,
        Instant franjaHasta,
        Instant expiraEn,
        Instant ingresoEn,
        String motivoRechazo,
        Instant creadoEn,
        /** Contenido a renderizar en el QR (datos embebidos en base64). */
        String qrPayload
) {
    public static VisitaResponse from(Visita v, String propiedadIdentificador, String qrPayload) {
        return new VisitaResponse(
                v.getId(), v.getCodigo(), v.getNombreVisitante(), v.getDocumento(),
                v.getPlaca(), v.getMotivo(), v.getCantidadPersonas(), v.getAcompanantes(),
                v.getPropiedadId(), propiedadIdentificador, v.getEstado(),
                v.getFranjaDesde(), v.getFranjaHasta(), v.getExpiraEn(), v.getIngresoEn(),
                v.getMotivoRechazo(), v.getCreadoEn(), qrPayload);
    }
}
