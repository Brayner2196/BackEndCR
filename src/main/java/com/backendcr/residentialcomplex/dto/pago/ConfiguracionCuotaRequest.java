package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.enums.Periodicidad;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfiguracionCuotaRequest(
        Long tipoPropiedadId,
        Long propiedadId,
        Integer numeroDesde,
        Integer numeroHasta,
        @NotNull @DecimalMin("0.01") BigDecimal monto,
        @NotNull Periodicidad periodicidad,
        @NotNull LocalDate fechaVigenciaDesde
) {}
