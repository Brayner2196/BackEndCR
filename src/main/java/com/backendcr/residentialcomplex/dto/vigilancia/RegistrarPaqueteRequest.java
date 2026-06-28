package com.backendcr.residentialcomplex.dto.vigilancia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registro de un paquete recibido en portería para una propiedad.
 */
public record RegistrarPaqueteRequest(
        @NotNull Long propiedadId,
        @NotBlank @Size(max = 200) String descripcion,
        @Size(max = 120) String remitente,
        @Size(max = 80) String transportadora
) {}
