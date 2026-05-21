package com.backendcr.residentialcomplex.tenant.dto;

import jakarta.validation.constraints.NotBlank;

public record ActualizarTenantRequest(

        @NotBlank
        String nombre,

        @NotBlank
        String codigo,

        String direccion,

        Boolean activo,

        /** Ej: "America/Bogota". Null = mantener la timezone actual. */
        String timezone
) {}
