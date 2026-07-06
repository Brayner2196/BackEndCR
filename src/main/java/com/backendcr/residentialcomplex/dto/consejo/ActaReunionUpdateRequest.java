package com.backendcr.residentialcomplex.dto.consejo;

/**
 * Actualización de un acta en estado BORRADOR.
 * Solo título y contenido son editables; la transcripción cruda es inmutable.
 */
public record ActaReunionUpdateRequest(
        String titulo,
        String contenido
) {}
