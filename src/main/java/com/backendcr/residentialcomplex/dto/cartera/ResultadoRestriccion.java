package com.backendcr.residentialcomplex.dto.cartera;

/**
 * Resultado de evaluar si una propiedad puede realizar una acción según su
 * estado de cartera.
 */
public record ResultadoRestriccion(
        boolean permitido,
        String estadoCodigo,
        String estadoNombre,
        String mensaje
) {
    /** Acción permitida (sin restricción o sin configuración). */
    public static ResultadoRestriccion permitido() {
        return new ResultadoRestriccion(true, null, null, null);
    }

    /** Acción bloqueada por el estado de cartera. */
    public static ResultadoRestriccion bloqueado(String estadoCodigo, String estadoNombre, String mensaje) {
        return new ResultadoRestriccion(false, estadoCodigo, estadoNombre, mensaje);
    }
}
