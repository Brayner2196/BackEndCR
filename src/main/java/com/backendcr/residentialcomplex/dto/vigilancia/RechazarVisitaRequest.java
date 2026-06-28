package com.backendcr.residentialcomplex.dto.vigilancia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Motivo obligatorio cuando el vigilante rechaza el ingreso de una visita.
 */
public record RechazarVisitaRequest(
        @NotBlank @Size(max = 300) String motivo
) {}
