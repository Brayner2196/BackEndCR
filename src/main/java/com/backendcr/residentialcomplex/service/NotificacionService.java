package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.FcmTokenRequest;
import com.backendcr.residentialcomplex.entity.DeviceToken;
import com.backendcr.residentialcomplex.repository.DeviceTokenRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final DeviceTokenRepository deviceTokenRepository;

    public NotificacionService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    // ─── Gestión de tokens ────────────────────────────────────────────────────

    @Transactional
    public void registrarToken(Long usuarioId, String tenantId, FcmTokenRequest request) {
        deviceTokenRepository.findByUsuarioIdAndPlataforma(usuarioId, request.getPlataforma())
            .ifPresentOrElse(
                existing -> existing.setToken(request.getToken()),
                () -> deviceTokenRepository.save(
                    new DeviceToken(usuarioId, tenantId, request.getToken(), request.getPlataforma())
                )
            );
    }

    @Transactional
    public void eliminarTokensDeUsuario(Long usuarioId) {
        deviceTokenRepository.deleteByUsuarioId(usuarioId);
    }

    // ─── Envío de notificaciones ──────────────────────────────────────────────

    /**
     * Envía una notificación push a todos los dispositivos de un usuario.
     */
    public void enviarAUsuario(Long usuarioId, String titulo, String cuerpo, Map<String, String> datos) {
        if (!firebaseDisponible()) return;

        List<DeviceToken> tokens = deviceTokenRepository.findByUsuarioId(usuarioId);
        enviarATokens(tokens, titulo, cuerpo, datos);
    }

    /**
     * Envía una notificación push a múltiples usuarios (ej: todos los del conjunto).
     */
    public void enviarAUsuarios(List<Long> usuarioIds, String titulo, String cuerpo, Map<String, String> datos) {
        if (!firebaseDisponible()) return;

        List<DeviceToken> tokens = deviceTokenRepository.findByUsuarioIdIn(usuarioIds);
        enviarATokens(tokens, titulo, cuerpo, datos);
    }

    /**
     * Envía a todos los usuarios de un tenant (broadcast del conjunto).
     */
    public void enviarATenant(String tenantId, String titulo, String cuerpo, Map<String, String> datos) {
        if (!firebaseDisponible()) return;

        List<DeviceToken> tokens = deviceTokenRepository.findByTenantId(tenantId);
        enviarATokens(tokens, titulo, cuerpo, datos);
    }

    // ─── Internos ─────────────────────────────────────────────────────────────

    private void enviarATokens(List<DeviceToken> deviceTokens, String titulo, String cuerpo, Map<String, String> datos) {
        if (deviceTokens.isEmpty()) {
            log.warn("FCM: no hay tokens registrados para enviar la notificación '{}'", titulo);
            return;
        }

        log.info("FCM: intentando enviar '{}' a {} dispositivos", titulo, deviceTokens.size());

        List<Message> mensajes = deviceTokens.stream()
            .map(dt -> construirMensaje(dt.getToken(), titulo, cuerpo, datos))
            .toList();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEach(mensajes);
            log.info("FCM: {} enviados, {} fallidos de {} tokens",
                response.getSuccessCount(), response.getFailureCount(), mensajes.size());

            response.getResponses().forEach(r -> {
                if (!r.isSuccessful()) {
                    log.warn("FCM token inválido o expirado: {}", r.getException().getMessage());
                }
            });
        } catch (FirebaseMessagingException e) {
            log.error("FCM error al enviar batch: {}", e.getMessage(), e);
        }
    }

    private Message construirMensaje(String token, String titulo, String cuerpo, Map<String, String> datos) {
        Message.Builder builder = Message.builder()
            .setToken(token)
            .setNotification(Notification.builder()
                .setTitle(titulo)
                .setBody(cuerpo)
                .build());

        if (datos != null && !datos.isEmpty()) {
            builder.putAllData(datos);
        }

        return builder.build();
    }

    private boolean firebaseDisponible() {
        boolean disponible = !FirebaseApp.getApps().isEmpty();
        if (!disponible) {
            log.warn("FCM: Firebase no inicializado — verifica la variable FIREBASE_CREDENTIALS_JSON en Railway.");
        }
        return disponible;
    }

    /** Diagnóstico: retorna el estado de Firebase y la cantidad de tokens en DB. */
    public Map<String, Object> diagnostico(Long usuarioId) {
        boolean inicializado = !FirebaseApp.getApps().isEmpty();
        List<DeviceToken> tokens = deviceTokenRepository.findByUsuarioId(usuarioId);
        return Map.of(
            "firebaseInicializado", inicializado,
            "tokensEnDb", tokens.size(),
            "tokens", tokens.stream().map(t -> Map.of(
                "plataforma", t.getPlataforma(),
                "tokenParcial", t.getToken().substring(0, Math.min(20, t.getToken().length())) + "...",
                "actualizadoEn", t.getActualizadoEn() != null ? t.getActualizadoEn().toString() : "N/A"
            )).toList()
        );
    }
}
