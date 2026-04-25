package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.enums.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoRequest(
        @NotNull Long cobroId,
        @NotNull @DecimalMin("0.01") BigDecimal montoPagado,
        @NotNull LocalDate fechaPago,
        @NotNull MetodoPago metodoPago,
        String referencia,
        String urlComprobante,
        String notas
) {}
