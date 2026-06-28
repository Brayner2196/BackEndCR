package com.backendcr.residentialcomplex.dto.vigilancia;

import java.util.List;

/**
 * Página de propiedades facturables para el selector con buscador del vigilante.
 */
public record PropiedadOpcionPage(
        List<PropiedadOpcionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {}
