package com.backendcr.residentialcomplex.dto.propiedad;

import jakarta.validation.constraints.NotBlank;

/**
 * Valor permitido del catálogo de un tipo de propiedad.
 * Se usa tanto para gestión (admin) como para alimentar los dropdowns del
 * registro/creación de propiedades.
 */
public record ValorTipoPropiedadDto(
        Long id,

        Long tipoId,

        @NotBlank
        String valor,

        /** null → plantilla global; no null → excepción bajo ese valor padre. */
        Long parentValorId,

        int orden,

        boolean activo
) {}
