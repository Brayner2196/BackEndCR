package com.backendcr.residentialcomplex.dto.propiedad;

public record ResidenteResumenDto(
        Long usuarioId,
        String nombre,
        String email,
        boolean esPrincipal
) {}
