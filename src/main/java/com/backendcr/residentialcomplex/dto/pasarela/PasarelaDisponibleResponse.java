package com.backendcr.residentialcomplex.dto.pasarela;

import com.backendcr.residentialcomplex.entity.TenantPasarela;
import com.backendcr.residentialcomplex.entity.enums.TipoPasarela;

/**
 * DTO que ve el residente: solo tipo, nombre y prioridad.
 * Sin credenciales ni datos sensibles.
 */
public record PasarelaDisponibleResponse(
        TipoPasarela tipo,
        String nombre,
        int prioridad
) {
    public static PasarelaDisponibleResponse from(TenantPasarela p) {
        return new PasarelaDisponibleResponse(
                p.getTipoPasarela(),
                nombreLegible(p.getTipoPasarela()),
                p.getPrioridad()
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
