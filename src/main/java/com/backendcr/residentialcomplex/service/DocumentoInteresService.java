package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.documento.ArchivoDescarga;
import com.backendcr.residentialcomplex.dto.documento.CambiarEstadoDocumentoRequest;
import com.backendcr.residentialcomplex.dto.documento.DocumentoInteresRequest;
import com.backendcr.residentialcomplex.dto.documento.DocumentoInteresResponse;
import com.backendcr.residentialcomplex.entity.ArchivoDocumento;
import com.backendcr.residentialcomplex.entity.DocumentoInteres;
import com.backendcr.residentialcomplex.entity.enums.CategoriaDocumento;
import com.backendcr.residentialcomplex.entity.enums.EstadoDocumento;
import com.backendcr.residentialcomplex.entity.enums.TipoArchivo;
import com.backendcr.residentialcomplex.exception.ResourceNotFoundException;
import com.backendcr.residentialcomplex.repository.ArchivoDocumentoRepository;
import com.backendcr.residentialcomplex.repository.DocumentoInteresRepository;
import com.backendcr.residentialcomplex.service.storage.StorageService;
import com.backendcr.residentialcomplex.validation.ArchivoDocumentoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Lógica de negocio de los documentos de interés general.
 *
 * Gestión (crear, editar, publicar, subir/eliminar archivos, borrar) → administrador.
 * Consulta y descarga de documentos PUBLICADOS → residentes.
 *
 * Los binarios viven en el bucket S3 vía {@link StorageService}; aquí solo se
 * persisten las keys y su metadata.
 */
@Service
@RequiredArgsConstructor
public class DocumentoInteresService {

    private static final String MODULO_STORAGE = "documentos";

    private final DocumentoInteresRepository documentoRepo;
    private final ArchivoDocumentoRepository archivoRepo;
    private final StorageService storageService;
    private final ArchivoDocumentoValidator archivoValidator;

    @Value("${documentos.max-archivos:10}")
    private long maxArchivosPorDocumento;

    // ─── Lectura: administrador (cualquier estado) ───────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentoInteresResponse> listarTodos(CategoriaDocumento categoria) {
        List<DocumentoInteres> docs = (categoria != null)
                ? documentoRepo.findAllByCategoriaOrderByCreadoEnDesc(categoria)
                : documentoRepo.findAllByOrderByCreadoEnDesc();
        return docs.stream().map(DocumentoInteresResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DocumentoInteresResponse obtenerAdmin(Long id) {
        return DocumentoInteresResponse.from(buscar(id));
    }

    // ─── Lectura: residente (solo PUBLICADO) ─────────────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentoInteresResponse> listarPublicados(CategoriaDocumento categoria) {
        List<DocumentoInteres> docs = (categoria != null)
                ? documentoRepo.findAllByEstadoAndCategoriaOrderByCreadoEnDesc(EstadoDocumento.PUBLICADO, categoria)
                : documentoRepo.findAllByEstadoOrderByCreadoEnDesc(EstadoDocumento.PUBLICADO);
        return docs.stream().map(DocumentoInteresResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DocumentoInteresResponse obtenerPublicado(Long id) {
        return DocumentoInteresResponse.from(buscarPublicado(id));
    }

    // ─── Escritura: administrador ────────────────────────────────────────────

    @Transactional
    public DocumentoInteresResponse crear(DocumentoInteresRequest req, Long usuarioId) {
        DocumentoInteres doc = new DocumentoInteres();
        doc.setTitulo(req.titulo());
        doc.setDescripcion(req.descripcion());
        doc.setCategoria(req.categoria());
        doc.setEstado(EstadoDocumento.BORRADOR);
        doc.setCreadoPor(usuarioId);
        return DocumentoInteresResponse.from(documentoRepo.save(doc));
    }

    @Transactional
    public DocumentoInteresResponse actualizar(Long id, DocumentoInteresRequest req) {
        DocumentoInteres doc = buscar(id);
        doc.setTitulo(req.titulo());
        doc.setDescripcion(req.descripcion());
        doc.setCategoria(req.categoria());
        return DocumentoInteresResponse.from(documentoRepo.save(doc));
    }

    @Transactional
    public DocumentoInteresResponse cambiarEstado(Long id, CambiarEstadoDocumentoRequest req) {
        DocumentoInteres doc = buscar(id);
        // No se debería publicar un documento sin archivos.
        if (req.estado() == EstadoDocumento.PUBLICADO && doc.getArchivos().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede publicar un documento sin archivos adjuntos");
        }
        doc.setEstado(req.estado());
        return DocumentoInteresResponse.from(documentoRepo.save(doc));
    }

    @Transactional
    public void eliminar(Long id) {
        DocumentoInteres doc = buscar(id);
        // Borra primero los binarios del bucket (best-effort), luego el registro (cascade borra los hijos).
        doc.getArchivos().forEach(a -> storageService.eliminar(a.getStorageKey()));
        documentoRepo.delete(doc);
    }

    // ─── Archivos adjuntos (administrador) ───────────────────────────────────

    @Transactional
    public DocumentoInteresResponse agregarArchivos(Long documentoId, List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe adjuntar al menos un archivo");
        }
        DocumentoInteres doc = buscar(documentoId);

        long existentes = doc.getArchivos().size();
        if (existentes + archivos.size() > maxArchivosPorDocumento) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Máximo " + maxArchivosPorDocumento + " archivos por documento (actuales: " + existentes + ")");
        }

        for (MultipartFile archivo : archivos) {
            TipoArchivo tipo = archivoValidator.validar(archivo);   // valida formato + tamaño
            String key = storageService.subir(archivo, MODULO_STORAGE);

            ArchivoDocumento nuevo = new ArchivoDocumento();
            nuevo.setStorageKey(key);
            nuevo.setNombreOriginal(archivo.getOriginalFilename());
            nuevo.setContentType(archivo.getContentType());
            nuevo.setTipo(tipo);
            nuevo.setTamanoBytes(archivo.getSize());
            doc.agregarArchivo(nuevo);
        }

        return DocumentoInteresResponse.from(documentoRepo.save(doc));
    }

    @Transactional
    public void eliminarArchivo(Long documentoId, Long archivoId) {
        ArchivoDocumento archivo = archivoRepo.findByIdAndDocumentoId(archivoId, documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo no encontrado"));
        storageService.eliminar(archivo.getStorageKey());
        archivoRepo.delete(archivo);
    }

    // ─── Descarga (admin: cualquier estado | residente: solo PUBLICADO) ──────

    @Transactional(readOnly = true)
    public ArchivoDescarga descargar(Long documentoId, Long archivoId, boolean soloPublicado) {
        DocumentoInteres doc = soloPublicado ? buscarPublicado(documentoId) : buscar(documentoId);
        ArchivoDocumento archivo = archivoRepo.findByIdAndDocumentoId(archivoId, doc.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Archivo no encontrado"));

        byte[] contenido = storageService.descargar(archivo.getStorageKey());
        String contentType = archivo.getContentType() != null
                ? archivo.getContentType()
                : storageService.obtenerContentType(archivo.getStorageKey());
        return new ArchivoDescarga(contenido, contentType, archivo.getNombreOriginal());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private DocumentoInteres buscar(Long id) {
        return documentoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
    }

    private DocumentoInteres buscarPublicado(Long id) {
        DocumentoInteres doc = buscar(id);
        if (doc.getEstado() != EstadoDocumento.PUBLICADO) {
            // Para el residente, un borrador es como si no existiera.
            throw new ResourceNotFoundException("Documento no encontrado");
        }
        return doc;
    }
}
