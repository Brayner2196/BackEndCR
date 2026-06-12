package com.backendcr.residentialcomplex.entity.enums;

/**
 * Catálogo de acciones que un estado de cartera puede prohibir a una propiedad.
 * Es extensible: agregar un valor no rompe la configuración existente (los
 * estados que no lo referencian simplemente no lo bloquean).
 */
public enum AccionRestringible {
    /** Crear una reserva de zona común. */
    RESERVAR_ZONA_COMUN,
    /** Ingreso de vehículo validado por el vigilante (placa). */
    ACCESO_VEHICULAR,
    /** Autorización de ingreso peatonal de visitantes. (Fase 2) */
    ACCESO_PEATONAL_VISITANTE,
    /** Descarga del certificado de paz y salvo. (Fase 2) */
    DESCARGAR_PAZ_Y_SALVO,
    /** Participación/voto en asambleas. (Fase 2) */
    VOTAR_ASAMBLEA,
    /** Publicar en el marketplace del conjunto. (Fase 2) */
    PUBLICAR_MARKETPLACE
}
