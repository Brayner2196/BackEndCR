package com.backendcr.residentialcomplex.dto.anuncio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnuncioRequest(
        @NotBlank @Size(max = 200) String titulo,
        @NotBlank @Size(max = 4000) String contenido,
        String imagenUrl,
        String fechaInicio,  // ISO-8601, opcional
        String fechaFin      // ISO-8601, opcional
) {}
