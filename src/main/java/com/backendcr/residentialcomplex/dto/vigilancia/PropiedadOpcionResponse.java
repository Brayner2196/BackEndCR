package com.backendcr.residentialcomplex.dto.vigilancia;

/**
 * Opción de propiedad para los selectores del vigilante (peatonal/paquetes).
 * Incluye el path corto (p. ej. "A101") para identificar la unidad de un vistazo.
 */
public record PropiedadOpcionResponse(
        Long id,
        String identificador,
        String pathCorto
) {}
