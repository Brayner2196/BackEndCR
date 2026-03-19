package com.backendcr.residentialcomplex.dto.usuario;

import com.backendcr.residentialcomplex.entity.enums.EstadoUsuario;

import jakarta.validation.constraints.NotBlank;

public record ActualizarUsuarioRequest(

        @NotBlank
        String nombre,

        String apto,

        String torre,

        String telefono,

        EstadoUsuario estado
) {}
