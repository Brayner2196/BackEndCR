package com.backendcr.residentialcomplex.config;

import com.backendcr.residentialcomplex.entity.TenantPasarela;
import com.backendcr.residentialcomplex.repository.TenantPasarelaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de rotación de clave maestra de cifrado.
 *
 * USO: cuando se necesita cambiar ENCRYPTION_SECRET sin perder datos.
 *
 * Pasos para rotar la clave en producción:
 * 1. Definir en env:  ENCRYPTION_SECRET_OLD = clave anterior
 *                     ENCRYPTION_SECRET_NEW = nueva clave generada con openssl rand -hex 32
 * 2. Invocar el endpoint protegido POST /api/admin/encryption/rotate  (solo SUPER_ADMIN)
 * 3. Verificar que los datos se lean correctamente
 * 4. Retirar ENCRYPTION_SECRET_OLD del entorno y renombrar NEW a ENCRYPTION_SECRET
 *
 * El proceso es atómico por fila (transacción): si falla a mitad, se hace rollback.
 */
@Slf4j
@Service
public class EncryptionKeyRotationService {

    private final TenantPasarelaRepository pasarelaRepo;

    public EncryptionKeyRotationService(TenantPasarelaRepository pasarelaRepo) {
        this.pasarelaRepo = pasarelaRepo;
    }

    /**
     * Re-cifra TODAS las credenciales de pasarelas:
     * descifra con oldKey → re-cifra con newKey → persiste.
     *
     * @param oldSecret clave maestra actual (antes de rotación)
     * @param newSecret nueva clave maestra
     * @return número de registros migrados
     */
    @Transactional
    public int rotarClave(String oldSecret, String newSecret) {
        EncryptionService oldCipher = new EncryptionService(oldSecret);
        EncryptionService newCipher = new EncryptionService(newSecret);

        List<TenantPasarela> todas = pasarelaRepo.findAll();
        int migradas = 0;

        for (TenantPasarela pasarela : todas) {
            boolean modificado = false;

            // publicKey
            if (pasarela.getPublicKey() != null) {
                // getPublicKey() ya viene descifrado gracias al AttributeConverter con oldCipher,
                // pero aquí bypasseamos el converter leyendo raw y re-cifrando manualmente.
                // Por eso usamos la query nativa para no pasar por el converter.
            }

            // Re-cifrado manual (bypass al AttributeConverter que usa el cipher activo)
            String rawPublicKey   = decryptSafe(oldCipher, getRawColumn(pasarela.getId(), "public_key"));
            String rawPrivateKey  = decryptSafe(oldCipher, getRawColumn(pasarela.getId(), "private_key"));
            String rawWebhookSecret = decryptSafe(oldCipher, getRawColumn(pasarela.getId(), "webhook_secret"));

            if (rawPublicKey != null) {
                pasarela.setPublicKey(newCipher.encrypt(rawPublicKey));
                modificado = true;
            }
            if (rawPrivateKey != null) {
                pasarela.setPrivateKey(newCipher.encrypt(rawPrivateKey));
                modificado = true;
            }
            if (rawWebhookSecret != null) {
                pasarela.setWebhookSecret(newCipher.encrypt(rawWebhookSecret));
                modificado = true;
            }

            if (modificado) {
                // Guardamos el valor YA cifrado con newCipher;
                // necesitamos desactivar el @Convert temporalmente para no doble-cifrar.
                // La forma limpia: query nativa UPDATE directo.
                pasarelaRepo.updateCredencialesRaw(
                    pasarela.getId(),
                    rawPublicKey    != null ? newCipher.encrypt(rawPublicKey)    : null,
                    rawPrivateKey   != null ? newCipher.encrypt(rawPrivateKey)   : null,
                    rawWebhookSecret!= null ? newCipher.encrypt(rawWebhookSecret): null
                );
                migradas++;
                log.info("Rotación OK — pasarela id={} tenant={}", pasarela.getId(),
                         pasarela.getTenant() != null ? pasarela.getTenant().getId() : "?");
            }
        }

        log.info("Rotación de clave completada: {} registros migrados", migradas);
        return migradas;
    }

    private String decryptSafe(EncryptionService cipher, String encrypted) {
        if (encrypted == null || encrypted.isBlank()) return null;
        try {
            return cipher.decrypt(encrypted);
        } catch (Exception e) {
            log.warn("No se pudo descifrar un campo durante rotación (puede ya estar en nuevo formato): {}", e.getMessage());
            return null;
        }
    }

    /**
     * Lee el valor RAW de una columna cifrada sin pasar por el AttributeConverter.
     * Implementado en el repositorio con @Query nativa.
     */
    private String getRawColumn(Long pasarelaId, String column) {
        return switch (column) {
            case "public_key"      -> pasarelaRepo.findRawPublicKey(pasarelaId);
            case "private_key"     -> pasarelaRepo.findRawPrivateKey(pasarelaId);
            case "webhook_secret"  -> pasarelaRepo.findRawWebhookSecret(pasarelaId);
            default -> null;
        };
    }
}
