package com.backendcr.residentialcomplex.dto.vigilancia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud del residente para pre-registrar una visita. Genera el código QR.
 */
public record CrearVisitaRequest(
        @NotNull Long propiedadId,
        @NotBlank @Size(max = 120) String nombreVisitante,
        @Size(max = 30) String documento,
        @Size(max = 15) String placa,
        @Size(max = 200) String motivo,
        /** Horas de validez del QR; si es null usa la parametrización del conjunto. */
        Integer validezHoras
) {}
