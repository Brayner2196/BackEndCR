package com.backendcr.residentialcomplex.dto.cartera;

/**
 * Petición para notificar un aviso de cobranza.
 *
 * @param estadoCarteraId fase de cartera a la que refiere el aviso. Si es null
 *                        en un aviso individual, se usa la fase vigente de la
 *                        propiedad. En el envío masivo es obligatorio.
 * @param mensaje         texto personalizado. Si es null/vacío se arma uno por
 *                        defecto a partir del nombre de la fase.
 */
public record NotificarCarteraRequest(
        Long estadoCarteraId,
        String mensaje
) {}
