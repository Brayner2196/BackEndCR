package com.backendcr.residentialcomplex.dto.pasarela;

import com.backendcr.residentialcomplex.entity.enums.TipoPasarela;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request para iniciar un checkout.
 * El residente elige la pasarela (si el tenant tiene varias).
 */
public record CheckoutRequest(

        @NotNull
        TipoPasarela pasarela,

        /**
         * Monto opcional. Si es null se usa el montoPendiente del cobro.
         * Si es mayor al pendiente → el exceso se acumula como saldo a favor.
         * Si es menor → abono parcial.
         */
        BigDecimal monto
) {}
