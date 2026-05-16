package com.backendcr.residentialcomplex.dto.reserva;

import com.backendcr.residentialcomplex.entity.enums.TipoExcepcionZona;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExcepcionZonaComunRequest(
        /** Formato "yyyy-MM-dd" */
        @NotBlank String fecha,
        @NotNull TipoExcepcionZona tipo,
        /** Requerido si tipo == APERTURA_ESPECIAL. Formato "HH:mm" o "HH:mm:ss" */
        String horaApertura,
        /** Requerido si tipo == APERTURA_ESPECIAL. Formato "HH:mm" o "HH:mm:ss" */
        String horaCierre,
        @Size(max = 300) String motivo
) {}
