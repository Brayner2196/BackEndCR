package com.backendcr.residentialcomplex.service.acta;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.config.TenantClock;
import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.consejo.ActaReunionResponse;
import com.backendcr.residentialcomplex.dto.consejo.ActaReunionUpdateRequest;
import com.backendcr.residentialcomplex.entity.ActaReunion;
import com.backendcr.residentialcomplex.entity.enums.EstadoActa;
import com.backendcr.residentialcomplex.repository.ActaReunionRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Gestión de actas de reunión por voz.
 *
 * La autorización de cargo (solo PRESIDENTE) se aplica en el controller vía
 * @PreAuthorize + PermisoValidator.esPresidenteActivo (validación contra BD).
 * Este servicio asume que el caller ya fue autorizado.
 */
@Service
@RequiredArgsConstructor
public class ActaReunionService {

    private static final Logger log = LoggerFactory.getLogger(ActaReunionService.class);

    /** Formatos de audio aceptados (extensiones producidas por la app / comunes). */
    private static final Set<String> EXTENSIONES_PERMITIDAS =
            Set.of("m4a", "aac", "mp3", "wav", "ogg", "opus", "flac", "webm");

    private final ActaReunionRepository actaRepo;
    private final UsuarioRepository usuarioRepo;
    private final WhisperTranscripcionService whisperService;
    private final SecurityUtils securityUtils;

    @Value("${whisper.audio-dir:/app/data/actas-audio}")
    private String audioDir;

    // ─── Crear (subir audio y encolar transcripción) ─────────────────────────

    @Transactional
    public ActaReunionResponse crear(String email, String titulo, String fechaReunionIso,
                                     Integer duracionSegundos, MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo de audio es obligatorio");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título del acta es obligatorio");
        }

        String extension = extraerExtension(audio.getOriginalFilename());
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato de audio no soportado: ." + extension);
        }

        Long usuarioId = securityUtils.resolverUsuarioId(email);

        Instant fechaReunion = fechaReunionIso != null && !fechaReunionIso.isBlank()
                ? Instant.parse(fechaReunionIso)
                : TenantClock.ahora();

        // Guardar el audio en disco: {audioDir}/{tenant}/{uuid}.{ext}
        Path rutaAudio = guardarAudio(audio, extension);

        ActaReunion acta = new ActaReunion();
        acta.setTitulo(titulo.trim());
        acta.setFechaReunion(fechaReunion);
        acta.setEstado(EstadoActa.PROCESANDO);
        acta.setAudioPath(rutaAudio.toString());
        acta.setDuracionSegundos(duracionSegundos);
        acta.setCreadoPorUsuarioId(usuarioId);
        acta = actaRepo.save(acta);

        whisperService.encolarTranscripcion(acta.getId(), rutaAudio);

        return toResponse(acta);
    }

    // ─── Consultas ────────────────────────────────────────────────────────────

    public List<ActaReunionResponse> listar() {
        return actaRepo.findAllByOrderByFechaReunionDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public ActaReunionResponse obtener(Long id) {
        return toResponse(buscar(id));
    }

    // ─── Edición (solo BORRADOR) ──────────────────────────────────────────────

    @Transactional
    public ActaReunionResponse actualizar(Long id, ActaReunionUpdateRequest req) {
        ActaReunion acta = buscar(id);
        exigirEstado(acta, EstadoActa.BORRADOR, "Solo se puede editar un acta en borrador");

        if (req.titulo() != null && !req.titulo().isBlank()) {
            acta.setTitulo(req.titulo().trim());
        }
        if (req.contenido() != null) {
            acta.setContenido(req.contenido());
        }
        return toResponse(actaRepo.save(acta));
    }

    @Transactional
    public ActaReunionResponse finalizar(Long id) {
        ActaReunion acta = buscar(id);
        exigirEstado(acta, EstadoActa.BORRADOR, "Solo se puede finalizar un acta en borrador");

        if (acta.getContenido() == null || acta.getContenido().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El acta no tiene contenido para finalizar");
        }

        acta.setEstado(EstadoActa.FINALIZADA);
        acta.setFinalizadaEn(TenantClock.ahora());
        return toResponse(actaRepo.save(acta));
    }

    /** Reintenta la transcripción de un acta en ERROR (el audio sigue en disco). */
    @Transactional
    public ActaReunionResponse reintentar(Long id) {
        ActaReunion acta = buscar(id);
        exigirEstado(acta, EstadoActa.ERROR, "Solo se puede reintentar un acta con error");

        if (acta.getAudioPath() == null || !Files.exists(Path.of(acta.getAudioPath()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El audio original ya no está disponible. Graba el acta nuevamente.");
        }

        acta.setEstado(EstadoActa.PROCESANDO);
        acta.setErrorMensaje(null);
        acta = actaRepo.save(acta);

        whisperService.encolarTranscripcion(acta.getId(), Path.of(acta.getAudioPath()));
        return toResponse(acta);
    }

    @Transactional
    public void eliminar(Long id) {
        ActaReunion acta = buscar(id);
        if (acta.getEstado() == EstadoActa.FINALIZADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un acta finalizada no se puede eliminar");
        }
        if (acta.getEstado() == EstadoActa.PROCESANDO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Espera a que termine la transcripción antes de eliminar el acta");
        }

        borrarAudioSilencioso(acta.getAudioPath());
        actaRepo.delete(acta);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ActaReunion buscar(Long id) {
        return actaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Acta no encontrada con id: " + id));
    }

    private static void exigirEstado(ActaReunion acta, EstadoActa esperado, String mensaje) {
        if (acta.getEstado() != esperado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
        }
    }

    private Path guardarAudio(MultipartFile audio, String extension) {
        try {
            String tenant = TenantContext.getTenant();
            Path dir = Path.of(audioDir, tenant != null ? tenant : "desconocido");
            Files.createDirectories(dir);
            Path destino = dir.resolve(UUID.randomUUID() + "." + extension);
            audio.transferTo(destino.toFile());
            return destino;
        } catch (IOException e) {
            log.error("No se pudo guardar el audio del acta", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar el archivo de audio");
        }
    }

    private void borrarAudioSilencioso(String ruta) {
        if (ruta == null) return;
        try {
            Files.deleteIfExists(Path.of(ruta));
        } catch (IOException e) {
            log.warn("No se pudo borrar el audio {}: {}", ruta, e.getMessage());
        }
    }

    private static String extraerExtension(String nombre) {
        if (nombre == null || !nombre.contains(".")) return "";
        return nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase();
    }

    private ActaReunionResponse toResponse(ActaReunion a) {
        String nombre = usuarioRepo.findById(a.getCreadoPorUsuarioId())
                .map(u -> u.getNombre())
                .orElse("Usuario desconocido");

        return new ActaReunionResponse(
                a.getId(),
                a.getTitulo(),
                a.getFechaReunion(),
                a.getEstado(),
                a.getTranscripcion(),
                a.getContenido(),
                a.getDuracionSegundos(),
                a.getCreadoPorUsuarioId(),
                nombre,
                a.getErrorMensaje(),
                a.getFinalizadaEn(),
                a.getCreadoEn(),
                a.getActualizadoEn()
        );
    }
}
