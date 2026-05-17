package com.backendcr.residentialcomplex.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Convierte List<String> ↔ TEXT con valores separados por coma.
 * Ej: ["EFECTIVO","NEQUI"] ↔ "EFECTIVO,NEQUI"
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String SEPARADOR = ",";

    @Override
    public String convertToDatabaseColumn(List<String> lista) {
        if (lista == null || lista.isEmpty()) return "";
        return lista.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(SEPARADOR));
    }

    @Override
    public List<String> convertToEntityAttribute(String columna) {
        if (columna == null || columna.isBlank()) return Collections.emptyList();
        return Arrays.stream(columna.split(SEPARADOR))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
