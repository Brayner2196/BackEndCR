package com.backendcr.residentialcomplex.service.acta;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.entity.ActaReunion;
import com.backendcr.residentialcomplex.entity.enums.EstadoActa;
import com.backendcr.residentialcomplex.repository.ActaReunionRepository;

import jakarta.annotation.PreDestroy;

/**
 * Transcripción de audio a texto con Whisper LOCAL (faster-whisper vía Python).
 *
 * Diseño:
 * - Cola de un solo hilo: las transcripciones se procesan en serie para no
 *   saturar CPU/RAM del servidor (Whisper local es intensivo).
 * - Propagación manual de TenantContext al hilo de trabajo: el contexto es
 *   ThreadLocal y el worker corre fuera del request HTTP. Sin esto, Hibernate
 *   no resolvería el schema del tenant.
 * - No usa @Async ni @EnableAsync para no introducir configuración global
 *   que afecte otros sistemas del proyecto.
 *
 * El script se invoca como:
 *   {whisper.python-command} {whisper.script-path} <audio> --model <m> --language es
 * y debe imprimir la transcripción por stdout (ver scripts/whisper_transcribe.py).
 */
@Service
public class WhisperTranscripcionService {

    private static final Logger log = LoggerFactory.getLogger(WhisperTranscripcionService.class);

    private final ActaReunionRepository actaRepo;

    @Value("${whisper.python-command:python3}")
    private String pythonCommand;

    @Value("${whisper.script-path:/app/scripts/whisper_transcribe.py}")
    private String scriptPath;

    @Value("${whisper.model:small}")
    private String modelo;

    @Value("${whisper.language:es}")
    private String idioma;

    @Value("${whisper.timeout-minutes:120}")
    private long timeoutMinutos;

    /** Un solo worker: transcripciones en serie (Whisper local es pesado). */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "whisper-worker");
        t.setDaemon(true);
        return t;
    });

    public WhisperTranscripcionService(ActaReunionRepository actaRepo) {
        this.actaRepo = actaRepo;
    }

    /**
     * Encola la transcripción del acta. Retorna de inmediato; el estado del
     * acta pasa a BORRADOR (éxito) o ERROR (fallo) cuando el worker termina.
     */
    public void encolarTranscripcion(Long actaId, Path audioPath) {
        // Capturar el contexto del request ANTES de salir del hilo HTTP.
        final String tenant = TenantContext.getTenant();
        final String timezone = TenantContext.getTimezone();

        executor.submit(() -> {
            try {
                TenantContext.setTenant(tenant);
                TenantContext.setTimezone(timezone);
                procesar(actaId, audioPath);
            } catch (Exception e) {
                log.error("Fallo inesperado transcribiendo acta {} (tenant {})", actaId, tenant, e);
                marcarError(actaId, "Error interno durante la transcripción");
            } finally {
                TenantContext.clear();
            }
        });
    }

    // ─── Worker ───────────────────────────────────────────────────────────────

    private void procesar(Long actaId, Path audioPath) {
        log.info("Iniciando transcripción Whisper de acta {} ({})", actaId, audioPath);

        String transcripcion;
        try {
            transcripcion = ejecutarWhisper(audioPath);
        } catch (Exception e) {
            log.error("Whisper falló para acta {}: {}", actaId, e.getMessage());
            marcarError(actaId, acotar(e.getMessage(), 500));
            return;
        }

        if (transcripcion == null || transcripcion.isBlank()) {
            marcarError(actaId, "La transcripción llegó vacía. Verifica que el audio tenga voz audible.");
            return;
        }

        actaRepo.findById(actaId).ifPresent(acta -> {
            acta.setTranscripcion(transcripcion);
            // El contenido editable parte de la transcripción cruda.
            acta.setContenido(transcripcion);
            acta.setEstado(EstadoActa.BORRADOR);
            acta.setErrorMensaje(null);
            actaRepo.save(acta);
            log.info("Acta {} transcrita: {} caracteres", actaId, transcripcion.length());
        });
    }

    private String ejecutarWhisper(Path audioPath) throws IOException, InterruptedException {
        if (!Files.exists(audioPath)) {
            throw new IOException("Archivo de audio no encontrado: " + audioPath);
        }

        ProcessBuilder pb = new ProcessBuilder(
                pythonCommand, scriptPath,
                audioPath.toAbsolutePath().toString(),
                "--model", modelo,
                "--language", idioma
        );
        pb.redirectErrorStream(false);

        Process proceso = pb.start();

        // Leer stdout/stderr en hilos aparte: si se leyera en este hilo con
        // readAllBytes, un proceso colgado bloquearía la lectura y el timeout
        // de waitFor nunca se aplicaría.
        var stdoutFuture = CompletableFuture.supplyAsync(() -> leerStream(proceso.getInputStream()));
        var stderrFuture = CompletableFuture.supplyAsync(() -> leerStream(proceso.getErrorStream()));

        boolean termino = proceso.waitFor(timeoutMinutos, TimeUnit.MINUTES);
        if (!termino) {
            proceso.destroyForcibly();
            throw new IOException("Whisper excedió el tiempo máximo de " + timeoutMinutos + " minutos");
        }

        String stdout = stdoutFuture.join();
        String stderr = stderrFuture.join();

        if (proceso.exitValue() != 0) {
            log.error("Whisper exit={} stderr: {}", proceso.exitValue(), acotar(stderr, 2000));
            throw new IOException("El proceso Whisper terminó con error (exit=" + proceso.exitValue() + ")");
        }

        return stdout.trim();
    }

    private static String leerStream(java.io.InputStream is) {
        try (is) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private void marcarError(Long actaId, String mensaje) {
        try {
            actaRepo.findById(actaId).ifPresent(acta -> {
                acta.setEstado(EstadoActa.ERROR);
                acta.setErrorMensaje(mensaje != null ? mensaje : "Error desconocido");
                actaRepo.save(acta);
            });
        } catch (Exception e) {
            log.error("No se pudo marcar ERROR en acta {}", actaId, e);
        }
    }

    private static String acotar(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    @PreDestroy
    void apagar() {
        executor.shutdown();
    }
}
