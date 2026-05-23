package com.backendcr.residentialcomplex.dto.presupuesto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GastoRegistradoRequest(

        @NotNull
        Long categoriaId,

        @NotBlank @Size(max = 300)
        String descripcion,

        @NotNull @DecimalMin("0.01")
        BigDecimal monto,

        @NotNull
        LocalDate fecha,

        /** URL o referencia del comprobante. Opcional. */
        @Size(max = 500)
        String comprobante
) {}
