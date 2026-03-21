package com.backendcr.residentialcomplex.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CrearTenantRequest(

        @NotBlank
        @Pattern(regexp = "^[a-z0-9_]+$",
                 message = "El schemaName solo puede contener letras minúsculas, números y guiones bajos")
        String schemaName,

        @NotBlank
        String nombre,

        @NotBlank
        String codigo,

        @NotBlank @Email
        String emailAdmin,

        @NotBlank
        String passwordAdmin,

        String direccion
) {}
