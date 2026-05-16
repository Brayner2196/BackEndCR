package com.backendcr.residentialcomplex.dto.reserva;

import com.backendcr.residentialcomplex.entity.enums.TipoExcepcionZona;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record ExcepcionZonaComunRequest(
        @NotNull LocalDate fecha,
        @NotNull TipoExcepcionZona tipo,
        /** Requerido si tipo == APERTURA_ESPECIAL */
        LocalTime horaApertura,
        /** Requerido si tipo == APERTURA_ESPECIAL */
        LocalTime horaCierre,
        @Size(max = 300) String motivo
) {}
