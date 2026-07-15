package com.backendcr.residentialcomplex.entity.enums;

/**
 * Tipo lógico de un archivo adjunto, derivado de su extensión/content-type.
 * Sirve para mostrar el ícono adecuado en la app y aplicar límites de tamaño por tipo.
 */
public enum TipoArchivo {
    PDF,
    WORD,
    EXCEL,
    IMAGEN,
    VIDEO
}
