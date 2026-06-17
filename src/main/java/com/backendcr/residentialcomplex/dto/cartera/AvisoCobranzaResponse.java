package com.backendcr.residentialcomplex.dto.cartera;

import com.backendcr.residentialcomplex.entity.AvisoCobranza;

/**
 * Resultado de un envío de aviso de cobranza.
 */
public record AvisoCobranzaResponse(
        Long propiedadId,
        String faseNombre,
        int usuariosNotificados,
        boolean enviado
) {
    public static AvisoCobranzaResponse from(AvisoCobranza a, String faseNombre) {
        return new AvisoCobranzaResponse(
                a.getPropiedadId(),
                faseNombre,
                a.getUsuariosNotificados(),
                a.getUsuariosNotificados() > 0
        );
    }
}
