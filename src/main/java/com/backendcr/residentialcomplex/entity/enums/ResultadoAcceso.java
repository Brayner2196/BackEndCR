package com.backendcr.residentialcomplex.entity.enums;

/**
 * Resultado de un evento de acceso para la bitácora de vigilancia.
 */
public enum ResultadoAcceso {
    /** Acceso autorizado. */
    PERMITIDO,
    /** Acceso negado (p. ej. por restricción de cartera). */
    DENEGADO,
    /** Evento informativo sin decisión de acceso (p. ej. recepción de paquete). */
    INFORMATIVO
}
