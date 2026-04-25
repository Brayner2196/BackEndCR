package com.backendcr.residentialcomplex.dto.pago;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PeriodoCobroRequest(
        @Min(2020) int anio,
        @Min(1) @Max(12) int mes,
        @NotNull LocalDate fechaInicio,
        @NotNull LocalDate fechaFin,
        @NotNull LocalDate fechaLimitePago
) {}
