package com.backendcr.residentialcomplex.dto.vigilancia;

import com.backendcr.residentialcomplex.entity.Paquete;
import com.backendcr.residentialcomplex.entity.enums.EstadoPaquete;

import java.time.Instant;

public record PaqueteResponse(
        Long id,
        Long propiedadId,
        String propiedadIdentificador,
        String descripcion,
        String remitente,
        String transportadora,
        EstadoPaquete estado,
        Instant recibidoEn,
        Instant entregadoEn,
        String entregadoA
) {
    public static PaqueteResponse from(Paquete p, String propiedadIdentificador) {
        return new PaqueteResponse(
                p.getId(), p.getPropiedadId(), propiedadIdentificador, p.getDescripcion(),
                p.getRemitente(), p.getTransportadora(), p.getEstado(),
                p.getRecibidoEn(), p.getEntregadoEn(), p.getEntregadoA());
    }
}
