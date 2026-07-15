package com.backendcr.residentialcomplex.dto.documento;

import com.backendcr.residentialcomplex.entity.ArchivoDocumento;
import com.backendcr.residentialcomplex.entity.enums.TipoArchivo;

/**
 * Metadata de un archivo adjunto expuesta al cliente.
 * No incluye la storageKey (dato interno): la descarga se hace por el endpoint dedicado.
 */
public record ArchivoDocumentoResponse(
        Long id,
        String nombreOriginal,
        String contentType,
        TipoArchivo tipo,
        Long tamanoBytes,
        String creadoEn
) {
    public static ArchivoDocumentoResponse from(ArchivoDocumento a) {
        return new ArchivoDocumentoResponse(
                a.getId(),
                a.getNombreOriginal(),
                a.getContentType(),
                a.getTipo(),
                a.getTamanoBytes(),
                a.getCreadoEn() != null ? a.getCreadoEn().toString() : null
        );
    }
}
