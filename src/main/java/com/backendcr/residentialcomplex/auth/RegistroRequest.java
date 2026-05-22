package com.backendcr.residentialcomplex.auth;

import java.util.List;

import com.backendcr.residentialcomplex.dto.propiedad.PropiedadPathItemDto;

import com.backendcr.residentialcomplex.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistroRequest(

        @NotBlank
        String nombre,

        @NotBlank @Email
        String email,

        @NotBlank
        @ValidPassword
        String password,

        @NotBlank
        String codigoConjunto,

        String telefono,

        List<PropiedadPathItemDto> propiedadPath

) {}
