package com.backendcr.residentialcomplex.dto.vigilancia;

import jakarta.validation.constraints.Size;

/**
 * Datos de la entrega de un paquete al residente.
 */
public record EntregarPaqueteRequest(
        @Size(max = 120) String entregadoA
) {}
