package com.backendcr.residentialcomplex.dto.consejo;

import com.backendcr.residentialcomplex.entity.enums.CargoConsejo;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MiembroConsejoRequest(
        @NotNull(message = "El usuarioId es obligatorio")
        Long usuarioId,

        @NotNull(message = "El cargo es obligatorio")
        CargoConsejo cargo,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        LocalDate fechaFin  // null = sin fecha de fin definida
) {}
