package com.backendcr.residentialcomplex.dto.propiedad;

public record UsuarioPropiedadResponse(
        Long id,
        Long propiedadId,
        String pathTexto,
        String nombreTipoRaiz,
        boolean esPrincipal
) {}
