package com.backendcr.residentialcomplex.entity.enums;

/**
 * Define cómo se gestiona un parqueadero PRIVADO en el conjunto.
 *
 * INDEPENDIENTE → el parqueadero es una propiedad facturable por sí misma
 *                 (tiene su propio TipoPropiedad con esParqueadero=true).
 *                 propiedadParqueaderoId apunta a esa propiedad.
 *                 Puede pertenecer a alguien sin apartamento.
 *
 * ACCESORIO     → el parqueadero es un complemento de un apartamento.
 *                 propiedadId apunta al apartamento dueño.
 *                 No genera cobro de administración propio.
 */
public enum ModeloParqueaderoPrivado {
    INDEPENDIENTE,
    ACCESORIO
}
