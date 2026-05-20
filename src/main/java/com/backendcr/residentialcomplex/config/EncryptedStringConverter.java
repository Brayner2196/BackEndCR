package com.backendcr.residentialcomplex.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter que cifra/descifra automáticamente columnas String
 * marcadas con @Convert(converter = EncryptedStringConverter.class).
 *
 * NOTA DE DISEÑO:
 * JPA instancia los converters con constructor vacío (los gestiona el EntityManager,
 * no Spring). Para poder usar EncryptionService (bean de Spring), se guarda en un
 * campo estático que Spring llena via @Autowired al arrancar el contexto.
 * El campo estático es seguro aquí porque EncryptionService es stateless (solo
 * tiene la SecretKey inmutable cargada al inicio).
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static EncryptionService ENCRYPTION_SERVICE;

    /** Constructor vacío requerido por JPA */
    public EncryptedStringConverter() {}

    /** Spring llena el campo estático cuando crea el bean @Component */
    @Autowired
    public void setEncryptionService(EncryptionService encryptionService) {
        ENCRYPTION_SERVICE = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (ENCRYPTION_SERVICE == null || plainText == null) return plainText;
        return ENCRYPTION_SERVICE.encrypt(plainText);
    }

    @Override
    public String convertToEntityAttribute(String encryptedValue) {
        if (ENCRYPTION_SERVICE == null || encryptedValue == null) return encryptedValue;
        return ENCRYPTION_SERVICE.decrypt(encryptedValue);
    }
}
