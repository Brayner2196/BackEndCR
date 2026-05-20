package com.backendcr.residentialcomplex.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter que cifra/descifra automáticamente columnas String
 * marcadas con @Convert(converter = EncryptedStringConverter.class).
 *
 * El cifrado usa AES-256-GCM vía EncryptionService (clave desde ENCRYPTION_SECRET).
 * JPA llama a convertToDatabaseColumn() antes de cada INSERT/UPDATE
 * y a convertToEntityAttribute() al leer de la BD.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final EncryptionService encryptionService;

    public EncryptedStringConverter(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String plainText) {
        return encryptionService.encrypt(plainText);
    }

    @Override
    public String convertToEntityAttribute(String encryptedValue) {
        return encryptionService.decrypt(encryptedValue);
    }
}
