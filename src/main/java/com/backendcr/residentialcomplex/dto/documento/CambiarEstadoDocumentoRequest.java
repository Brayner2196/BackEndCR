package com.backendcr.residentialcomplex.dto.documento;

import com.backendcr.residentialcomplex.entity.enums.EstadoDocumento;
import jakarta.validation.constraints.NotNull;

/** Cambia el estado de publicación de un documento (BORRADOR / PUBLICADO). */
public record CambiarEstadoDocumentoRequest(
        @NotNull EstadoDocumento estado
) {}
