package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.dto.documento.ArchivoDescarga;
import com.backendcr.residentialcomplex.dto.documento.CambiarEstadoDocumentoRequest;
import com.backendcr.residentialcomplex.dto.documento.DocumentoInteresRequest;
import com.backendcr.residentialcomplex.dto.documento.DocumentoInteresResponse;
import com.backendcr.residentialcomplex.entity.enums.CategoriaDocumento;
import com.backendcr.residentialcomplex.service.DocumentoInteresService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Gestión de documentos de interés general — solo TENANT_ADMIN.
 * Permite crear/editar documentos, subir y eliminar archivos, y publicarlos.
 */
@RestController
@RequestMapping("/api/admin/documentos")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class AdminDocumentoController {

    private final DocumentoInteresService documentoService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public List<DocumentoInteresResponse> listar(
            @RequestParam(required = false) CategoriaDocumento categoria) {
        return documentoService.listarTodos(categoria);
    }

    @GetMapping("/{id}")
    public DocumentoInteresResponse obtener(@PathVariable Long id) {
        return documentoService.obtenerAdmin(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoInteresResponse crear(@Valid @RequestBody DocumentoInteresRequest req,
                                          @AuthenticationPrincipal String email) {
        return documentoService.crear(req, securityUtils.resolverUsuarioId(email));
    }

    @PutMapping("/{id}")
    public DocumentoInteresResponse actualizar(@PathVariable Long id,
                                               @Valid @RequestBody DocumentoInteresRequest req) {
        return documentoService.actualizar(id, req);
    }

    @PutMapping("/{id}/estado")
    public DocumentoInteresResponse cambiarEstado(@PathVariable Long id,
                                                  @Valid @RequestBody CambiarEstadoDocumentoRequest req) {
        return documentoService.cambiarEstado(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        documentoService.eliminar(id);
    }

    // ─── Archivos adjuntos ────────────────────────────────────────────────────

    /** Sube uno o varios archivos al documento. multipart/form-data → campo "archivos". */
    @PostMapping(path = "/{id}/archivos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentoInteresResponse subirArchivos(@PathVariable Long id,
                                                  @RequestPart("archivos") List<MultipartFile> archivos) {
        return documentoService.agregarArchivos(id, archivos);
    }

    @DeleteMapping("/{id}/archivos/{archivoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarArchivo(@PathVariable Long id, @PathVariable Long archivoId) {
        documentoService.eliminarArchivo(id, archivoId);
    }

    /** Descarga un archivo (el admin puede descargar aunque esté en borrador). */
    @GetMapping("/{id}/archivos/{archivoId}/descargar")
    public ResponseEntity<Resource> descargar(@PathVariable Long id, @PathVariable Long archivoId) {
        ArchivoDescarga descarga = documentoService.descargar(id, archivoId, false);
        return construirRespuestaDescarga(descarga);
    }

    static ResponseEntity<Resource> construirRespuestaDescarga(ArchivoDescarga descarga) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(descarga.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + descarga.nombreOriginal() + "\"")
                .body(new ByteArrayResource(descarga.contenido()));
    }
}
