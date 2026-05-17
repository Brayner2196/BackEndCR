package com.backendcr.residentialcomplex.dto.solicitud;

import com.backendcr.residentialcomplex.entity.enums.TipoSolicitud;
import jakarta.validation.constraints.*;

public record SolicitudRequest(
        @NotNull Long publicacionId,
        @NotNull TipoSolicitud tipo,
        @NotNull @Min(1) @Max(999) int cantidad,
        @Size(max = 500) String notas
) {}
