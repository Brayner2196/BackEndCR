package com.backendcr.residentialcomplex.dto.propiedad;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record TipoPropiedadNodoDto(
        Long id,

        @NotBlank
        String nombre,

        String descripcion,

        Long parentId,

        int orden,

        boolean activo,

        boolean esFacturable,

        /** true → al crear una propiedad de este tipo se auto-crea un Parqueadero INDEPENDIENTE */
        boolean esParqueadero,

        List<TipoPropiedadNodoDto> hijos
) {}
