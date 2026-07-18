package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.dto.documento.ArchivoDescargaUrl;
import com.backendcr.residentialcomplex.dto.documento.DocumentoInteresResponse;
import com.backendcr.residentialcomplex.entity.enums.CategoriaDocumento;
import com.backendcr.residentialcomplex.service.DocumentoInteresService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consulta y descarga de documentos de interés general — propietarios e inquilinos.
 * Solo se exponen los documentos en estado PUBLICADO.
 */
@RestController
@RequestMapping("/api/residente/documentos")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'INQUILINO')")
@RequiredArgsConstructor
public class ResidenteDocumentoController {

    private final DocumentoInteresService documentoService;

    @GetMapping
    public List<DocumentoInteresResponse> listar(
            @RequestParam(required = false) CategoriaDocumento categoria) {
        return documentoService.listarPublicados(categoria);
    }

    @GetMapping("/{id}")
    public DocumentoInteresResponse obtener(@PathVariable Long id) {
        return documentoService.obtenerPublicado(id);
    }

    /** URL firmada para descargar un archivo de un documento publicado. */
    @GetMapping("/{id}/archivos/{archivoId}/url")
    public ArchivoDescargaUrl urlDescarga(@PathVariable Long id, @PathVariable Long archivoId) {
        return documentoService.generarUrlDescarga(id, archivoId, true);
    }
}
