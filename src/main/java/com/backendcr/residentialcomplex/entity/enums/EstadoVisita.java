package com.backendcr.residentialcomplex.entity.enums;

/**
 * Ciclo de vida de una visita pre-registrada por un residente.
 */
public enum EstadoVisita {
    /** Creada por el residente, aún sin ingresar. */
    PENDIENTE,
    /** El vigilante validó el QR y autorizó el ingreso. */
    INGRESO,
    /** Visita cerrada (registro de salida o expiración tras ingreso). */
    FINALIZADA,
    /** Expiró sin haber ingresado. */
    VENCIDA,
    /** Cancelada por el residente antes de ingresar. */
    CANCELADA,
    /** Rechazada por el vigilante al escanear (con motivo). */
    RECHAZADA
}
