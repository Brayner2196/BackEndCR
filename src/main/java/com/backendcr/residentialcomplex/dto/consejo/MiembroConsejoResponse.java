package com.backendcr.residentialcomplex.dto.consejo;

import com.backendcr.residentialcomplex.entity.enums.CargoConsejo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MiembroConsejoResponse(
        Long id,
        Long usuarioId,
        String nombreUsuario,
        CargoConsejo cargo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean activo,
        LocalDateTime creadoEn
) {}
