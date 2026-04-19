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

        List<TipoPropiedadNodoDto> hijos
) {}
