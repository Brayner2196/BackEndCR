package com.backendcr.residentialcomplex.dto.reserva;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record HorarioGrupoDto(
        Long id,
        @NotBlank String etiqueta,
        /** CSV de días: LUNES,MARTES,... */
        @NotNull String dias,
        String nota,
        int orden,
        List<FranjaHorariaDto> franjas
) {}
