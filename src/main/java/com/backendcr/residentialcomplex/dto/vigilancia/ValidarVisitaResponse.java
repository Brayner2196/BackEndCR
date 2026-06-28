package com.backendcr.residentialcomplex.dto.vigilancia;

import com.backendcr.residentialcomplex.entity.enums.EstadoVisita;

/**
 * Resultado de validar un QR de visita en portería.
 */
public record ValidarVisitaResponse(
        boolean permitido,
        EstadoVisita estado,
        String nombreVisitante,
        String documento,
        String placa,
        Long propiedadId,
        String propiedadIdentificador,
        String mensaje
) {}
