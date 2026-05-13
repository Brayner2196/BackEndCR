package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.enums.ConceptoCobro;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CobroEspecialRequest(
        @NotNull Long propiedadId,
        @NotNull ConceptoCobro concepto,
        @NotNull @Size(min = 3, max = 300) String descripcion,
        @NotNull @DecimalMin("0.01") BigDecimal monto,
        @NotNull LocalDate fechaLimitePago
) {}
