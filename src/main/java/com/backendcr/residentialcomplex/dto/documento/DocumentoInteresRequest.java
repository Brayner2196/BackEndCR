package com.backendcr.residentialcomplex.dto.documento;

import com.backendcr.residentialcomplex.entity.enums.CategoriaDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear o actualizar un documento de interés general (solo metadata,
 * los archivos se suben aparte por multipart).
 */
public record DocumentoInteresRequest(
        @NotBlank @Size(max = 200) String titulo,
        @Size(max = 2000) String descripcion,
        @NotNull CategoriaDocumento categoria
) {}
