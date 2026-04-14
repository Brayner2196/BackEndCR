package com.backendcr.residentialcomplex.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistroRequest(

        @NotBlank
        String nombre,

        @NotBlank @Email
        String email,

        @NotBlank
        String password,

        @NotBlank
        String codigoConjunto,

        String apto,

        String torre,

        String telefono        
        
) {}
