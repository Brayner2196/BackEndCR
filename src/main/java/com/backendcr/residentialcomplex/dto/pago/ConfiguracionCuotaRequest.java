package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.enums.Periodicidad;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfiguracionCuotaRequest(
        Long tipoPropiedadId,
        Long propiedadId,
        /**
         * Tipo de propiedad ancestro sobre el que se evalúa el rango numérico.
         * Null = el rango se aplica sobre el identificador de la propia propiedad facturable.
         * Ejemplo: si la regla es "pisos 1-10 pagan X", se pone aquí el id del TipoPropiedad PISO.
         */
        Long tipoPropiedadCondicionId,
        Integer numeroDesde,
        Integer numeroHasta,
        @NotNull @DecimalMin("0.01") BigDecimal monto,
        @NotNull Periodicidad periodicidad,
        @NotNull LocalDate fechaVigenciaDesde,
        /** Null = sin fecha de fin (vigente indefinidamente). */
        LocalDate fechaVigenciaHasta
) {}
