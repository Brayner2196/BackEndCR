package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.enums.TipoCalculoMora;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfiguracionMoraRequest(

        @NotNull(message = "El tipo de cálculo es requerido")
        TipoCalculoMora tipoCalculo,

        /** Solo requerido cuando tipoCalculo == PORCENTAJE */
        @DecimalMin(value = "0.01", inclusive = true,
                message = "El porcentaje mensual debe ser mayor a 0")
        @DecimalMax(value = "100.00",
                message = "El porcentaje mensual no puede exceder 100")
        BigDecimal porcentajeMensual,

        /** Solo requerido cuando tipoCalculo == MONTO_FIJO */
        @DecimalMin(value = "0.01", inclusive = true,
                message = "El monto fijo debe ser mayor a 0")
        BigDecimal montoFijo,

        @Min(value = 0, message = "Los días de gracia no pueden ser negativos")
        @Max(value = 60, message = "Los días de gracia no pueden superar 60")
        int diasGracia,

        @NotNull(message = "La fecha de vigencia es requerida")
        LocalDate fechaVigencia
) {}
