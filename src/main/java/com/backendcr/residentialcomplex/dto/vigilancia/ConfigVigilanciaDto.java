package com.backendcr.residentialcomplex.dto.vigilancia;

import com.backendcr.residentialcomplex.entity.ConfigVigilancia;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Parametrización del módulo de vigilancia (lectura y actualización).
 */
public record ConfigVigilanciaDto(
        @Min(1) @Max(168) int expiracionVisitaHoras,
        boolean exigeDocumentoPeatonal,
        boolean exigeFotoPaquete,
        boolean notificarLlegadaPaquete,
        boolean permitirAprobarConCarteraRestringida
) {
    public static ConfigVigilanciaDto from(ConfigVigilancia c) {
        return new ConfigVigilanciaDto(
                c.getExpiracionVisitaHoras(),
                c.isExigeDocumentoPeatonal(),
                c.isExigeFotoPaquete(),
                c.isNotificarLlegadaPaquete(),
                c.isPermitirAprobarConCarteraRestringida());
    }
}
