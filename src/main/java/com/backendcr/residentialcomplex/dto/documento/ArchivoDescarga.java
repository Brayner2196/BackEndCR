package com.backendcr.residentialcomplex.dto.documento;

/**
 * Contenido de un archivo listo para servir por el controller (descarga vía proxy).
 */
public record ArchivoDescarga(
        byte[] contenido,
        String contentType,
        String nombreOriginal
) {}
