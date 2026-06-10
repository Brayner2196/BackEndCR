package com.backendcr.residentialcomplex.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;


@Component
public class EncryptionService {

    private static final String ALGORITHM     = "AES/GCM/NoPadding";
    private static final int    IV_LENGTH     = 12;   // 96 bits — recomendado para GCM
    private static final int    TAG_LENGTH    = 128;  // bits

    private final SecretKey secretKey;

    public EncryptionService(@Value("${app.encryption.secret}") String secret) {
        // Tomar exactamente 32 bytes de la clave (pad o truncar)
        byte[] keyBytes = new byte[32];
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(secretBytes, 0, keyBytes, 0, Math.min(secretBytes.length, 32));
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Cifra un texto plano y devuelve Base64(iv + ciphertext).
     * Genera un IV aleatorio distinto en cada llamada (nunca reutilizar IV con GCM).
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) return plainText;
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Combinar: iv (12 bytes) + ciphertext+tag
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Error cifrando credencial", e);
        }
    }

    /**
     * Descifra un valor previamente cifrado con encrypt().
     */
    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) return encryptedBase64;
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);

            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Error descifrando credencial", e);
        }
    }
}
