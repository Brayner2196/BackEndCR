package com.backendcr.residentialcomplex.dto.conjunto;

public record MiConjuntoResponse(
        String nombre,
        String codigo,
        String direccion,
        boolean activo
) {}
