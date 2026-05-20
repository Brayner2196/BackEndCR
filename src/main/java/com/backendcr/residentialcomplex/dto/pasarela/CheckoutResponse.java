package com.backendcr.residentialcomplex.dto.pasarela;

import com.backendcr.residentialcomplex.entity.enums.TipoPasarela;

/**
 * Respuesta unificada del checkout para todas las pasarelas.
 * Flutter usa el campo 'tipo' para saber qué WebView/SDK abrir.
 */
public record CheckoutResponse(
        String checkoutUrl,
        TipoPasarela tipo
) {}
