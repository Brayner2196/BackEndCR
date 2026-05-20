package com.backendcr.residentialcomplex.dto.pasarela;

import com.backendcr.residentialcomplex.entity.TenantPasarela;
import com.backendcr.residentialcomplex.entity.enums.TipoPasarela;

/**
 * Response con la configuración de una pasarela.
 * NUNCA expone privateKey ni webhookSecret completos — solo indica si están seteados.
 */
public record PasarelaConfigResponse(
        Long id,
        TipoPasarela tipoPasarela,
        String nombre,
        boolean activa,
        int prioridad,
        boolean sandbox,
        boolean tienePublicKey,
        boolean tienePrivateKey,
        boolean tieneWebhookSecret
) {
    public static PasarelaConfigResponse from(TenantPasarela p) {
        return new PasarelaConfigResponse(
                p.getId(),
                p.getTipoPasarela(),
                nombreLegible(p.getTipoPasarela()),
                p.isActiva(),
                p.getPrioridad(),
                p.isSandbox(),
                p.getPublicKey() != null && !p.getPublicKey().isBlank(),
                p.getPrivateKey() != null && !p.getPrivateKey().isBlank(),
                p.getWebhookSecret() != null && !p.getWebhookSecret().isBlank()
        );
    }

    private static String nombreLegible(TipoPasarela tipo) {
        return switch (tipo) {
            case MERCADO_PAGO -> "Mercado Pago";
            case WOMPI -> "Wompi";
            case BOLD -> "Bold";
        };
    }
}
