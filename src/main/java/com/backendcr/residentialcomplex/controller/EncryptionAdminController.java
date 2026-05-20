package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.EncryptionKeyRotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint de rotación de clave maestra de cifrado.
 * Solo accesible para SUPER_ADMIN.
 *
 * FLUJO DE ROTACIÓN:
 * 1. Generar nueva clave: openssl rand -hex 32
 * 2. POST /api/super/encryption/rotate  { "oldSecret": "...", "newSecret": "..." }
 * 3. Verificar que la app sigue leyendo los datos
 * 4. Actualizar ENCRYPTION_SECRET en el entorno con la nueva clave y reiniciar
 */
@Slf4j
@RestController
@RequestMapping("/api/super/encryption")
@RequiredArgsConstructor
public class EncryptionAdminController {

    private final EncryptionKeyRotationService rotationService;

    /**
     * Rota la clave maestra re-cifrando todas las credenciales de pasarelas.
     *
     * Body: { "oldSecret": "clave_actual", "newSecret": "clave_nueva" }
     *
     * PRECAUCIÓN: ejecutar en ventana de mantenimiento, con backup de BD previo.
     */
    @PostMapping("/rotate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> rotarClave(@RequestBody Map<String, String> body) {
        String oldSecret = body.get("oldSecret");
        String newSecret = body.get("newSecret");

        if (oldSecret == null || oldSecret.isBlank() || newSecret == null || newSecret.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Se requieren oldSecret y newSecret"));
        }

        if (oldSecret.equals(newSecret)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "oldSecret y newSecret no pueden ser iguales"));
        }

        log.warn("Iniciando rotación de clave maestra de cifrado — operación sensible");

        int migradas = rotationService.rotarClave(oldSecret, newSecret);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Rotación completada exitosamente",
                "registrosMigrados", migradas,
                "instruccion", "Actualiza ENCRYPTION_SECRET en el entorno con el newSecret y reinicia la app"
        ));
    }
}
