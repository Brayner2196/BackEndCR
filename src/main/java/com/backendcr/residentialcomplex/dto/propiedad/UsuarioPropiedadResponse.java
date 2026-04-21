package com.backendcr.residentialcomplex.dto.propiedad;

import com.backendcr.residentialcomplex.entity.enums.EstadoPropiedad;

public record UsuarioPropiedadResponse(
        Long id,
        Long propiedadId,
        String pathTexto,
        String nombreTipoRaiz,
        EstadoPropiedad estadoPropiedad,
        boolean esPrincipal
) {}
