package com.backendcr.residentialcomplex.dto.documento;

/**
 * Enlace firmado (presigned) para que el cliente descargue un archivo directo del bucket.
 * La URL es temporal; el cliente debe usarla de inmediato.
 */
public record ArchivoDescargaUrl(
        String url,
        String nombreOriginal,
        String contentType
) {}
