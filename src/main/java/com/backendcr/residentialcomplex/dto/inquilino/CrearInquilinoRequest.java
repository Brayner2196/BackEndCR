package com.backendcr.residentialcomplex.dto.inquilino;

import com.backendcr.residentialcomplex.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearInquilinoRequest(

        @NotBlank
        @Size(min = 3, message = "El nombre debe tener al menos 3 caracteres")
        String nombre,

        @NotBlank @Email
        String email,

        @NotBlank
        @ValidPassword
        String password,

        String telefono
) {}
