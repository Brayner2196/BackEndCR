package com.backendcr.residentialcomplex.entity.enums;

public enum ModoAprobacion {
    /** Toda reserva se aprueba automáticamente. */
    AUTOMATICA,
    /** Toda reserva requiere aprobación manual. */
    MANUAL,
    /** Reglas condicionales determinan si es auto o manual. */
    MIXTA
}
