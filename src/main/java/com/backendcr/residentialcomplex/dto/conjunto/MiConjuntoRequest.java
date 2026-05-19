package com.backendcr.residentialcomplex.dto.conjunto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MiConjuntoRequest(
        @NotBlank(message = "El nombre es requerido")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
        String direccion
) {}
