package com.backendcr.residentialcomplex.tenant.dto;

import jakarta.validation.constraints.NotBlank;

public record ActualizarTenantRequest(

        @NotBlank
        String nombre,

        @NotBlank
        String codigo,

        String direccion,

        Boolean activo
) {}
