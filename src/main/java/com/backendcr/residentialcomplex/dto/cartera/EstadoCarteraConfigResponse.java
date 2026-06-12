package com.backendcr.residentialcomplex.dto.cartera;

import java.util.List;

/** Estado de cartera con su configuración completa (reglas + restricciones). */
public record EstadoCarteraConfigResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        int severidad,
        String color,
        boolean esPositivo,
        boolean activo,
        List<ReglaDto> reglas,
        List<RestriccionDto> restricciones
) {}
