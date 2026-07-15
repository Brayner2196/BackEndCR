package com.backendcr.residentialcomplex.validation;

import com.backendcr.residentialcomplex.entity.enums.TipoArchivo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

/**
 * Validador reutilizable para los archivos de documentos de interés general.
 *
 * Valida por extensión (whitelist) y por tamaño según el tipo. La duración de video
 * NO se valida aquí (requeriría procesar el binario con ffmpeg): debe limitarse en el
 * cliente Flutter antes de subir; el backend solo acota el peso.
 *
 * Devuelve el {@link TipoArchivo} resuelto para que el servicio lo persista.
 */
@Component
public class ArchivoDocumentoValidator {

    /** Extensiones permitidas mapeadas a su tipo lógico. */
    private static final Map<String, TipoArchivo> EXTENSIONES = Map.ofEntries(
            Map.entry("pdf",  TipoArchivo.PDF),
            Map.entry("doc",  TipoArchivo.WORD),
            Map.entry("docx", TipoArchivo.WORD),
            Map.entry("xls",  TipoArchivo.EXCEL),
            Map.entry("xlsx", TipoArchivo.EXCEL),
            Map.entry("csv",  TipoArchivo.EXCEL),
            Map.entry("jpg",  TipoArchivo.IMAGEN),
            Map.entry("jpeg", TipoArchivo.IMAGEN),
            Map.entry("png",  TipoArchivo.IMAGEN),
            Map.entry("webp", TipoArchivo.IMAGEN),
            Map.entry("mp4",  TipoArchivo.VIDEO),
            Map.entry("mov",  TipoArchivo.VIDEO),
            Map.entry("webm", TipoArchivo.VIDEO)
    );

    /** Tipos que comparten el límite "general" de tamaño (todo lo que no es video). */
    private static final Set<TipoArchivo> TIPOS_GENERALES =
            Set.of(TipoArchivo.PDF, TipoArchivo.WORD, TipoArchivo.EXCEL, TipoArchivo.IMAGEN);

    @Value("${documentos.max-size-general-mb:15}")
    private long maxGeneralMb;

    @Value("${documentos.max-size-video-mb:50}")
    private long maxVideoMb;

    /**
     * Valida el archivo y retorna su tipo. Lanza 400 si es inválido.
     */
    public TipoArchivo validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo es obligatorio");
        }

        String extension = extraerExtension(archivo.getOriginalFilename());
        TipoArchivo tipo = EXTENSIONES.get(extension);
        if (tipo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato no permitido. Permitidos: PDF, Word, Excel, imágenes y video (mp4/mov/webm)");
        }

        long maxBytes = (tipo == TipoArchivo.VIDEO ? maxVideoMb : maxGeneralMb) * 1024L * 1024L;
        if (archivo.getSize() > maxBytes) {
            long maxMb = tipo == TipoArchivo.VIDEO ? maxVideoMb : maxGeneralMb;
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo supera el tamaño máximo de " + maxMb + " MB para " + tipo);
        }

        return tipo;
    }

    private String extraerExtension(String nombreOriginal) {
        if (nombreOriginal == null) {
            return "";
        }
        int punto = nombreOriginal.lastIndexOf('.');
        if (punto < 0 || punto == nombreOriginal.length() - 1) {
            return "";
        }
        return nombreOriginal.substring(punto + 1).toLowerCase();
    }
}
