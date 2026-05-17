package com.backendcr.residentialcomplex.dto.usuario;

import com.backendcr.residentialcomplex.entity.enums.EstadoUsuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ActualizarUsuarioRequest(

        @NotBlank
        String nombre,

        String telefono,

        @NotNull
        EstadoUsuario estado
) {}
