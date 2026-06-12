package com.backendcr.residentialcomplex.dto.cartera;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Payload de creación/actualización de un estado de cartera con sus reglas y restricciones. */
public record EstadoCarteraConfigRequest(
        @NotBlank @Size(max = 40) String codigo,
        @NotBlank @Size(max = 80) String nombre,
        @Size(max = 300) String descripcion,
        Integer severidad,
        @Size(max = 9) String color,
        Boolean esPositivo,
        Boolean activo,
        List<ReglaDto> reglas,
        List<RestriccionDto> restricciones
) {}
