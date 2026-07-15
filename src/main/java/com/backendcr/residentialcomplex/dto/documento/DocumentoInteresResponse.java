package com.backendcr.residentialcomplex.dto.documento;

import com.backendcr.residentialcomplex.entity.DocumentoInteres;
import com.backendcr.residentialcomplex.entity.enums.CategoriaDocumento;
import com.backendcr.residentialcomplex.entity.enums.EstadoDocumento;

import java.util.List;

/** Documento de interés general con su lista de archivos adjuntos. */
public record DocumentoInteresResponse(
        Long id,
        String titulo,
        String descripcion,
        CategoriaDocumento categoria,
        EstadoDocumento estado,
        Long creadoPor,
        String creadoEn,
        String actualizadoEn,
        List<ArchivoDocumentoResponse> archivos
) {
    public static DocumentoInteresResponse from(DocumentoInteres d) {
        List<ArchivoDocumentoResponse> archivos = d.getArchivos() != null
                ? d.getArchivos().stream().map(ArchivoDocumentoResponse::from).toList()
                : List.of();
        return new DocumentoInteresResponse(
                d.getId(),
                d.getTitulo(),
                d.getDescripcion(),
                d.getCategoria(),
                d.getEstado(),
                d.getCreadoPor(),
                d.getCreadoEn() != null ? d.getCreadoEn().toString() : null,
                d.getActualizadoEn() != null ? d.getActualizadoEn().toString() : null,
                archivos
        );
    }
}
