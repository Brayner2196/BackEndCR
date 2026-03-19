package com.backendcr.residentialcomplex.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CrearUsuarioRequest(

        @NotBlank
        String nombre,

        @NotBlank @Email
        String email,

        @NotBlank
        String password,

        @NotBlank
        @Pattern(regexp = "TENANT_ADMIN|RESIDENTE|RESIDENTE_PENDIENTE|VIGILANTE|PORTERO|PISCINERO|CONTADOR",
                 message = "Rol no válido")
        String rol,

        String apto,

        String torre,

        String telefono
) {}
