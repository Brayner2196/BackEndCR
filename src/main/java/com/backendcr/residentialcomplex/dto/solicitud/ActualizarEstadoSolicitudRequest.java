package com.backendcr.residentialcomplex.dto.solicitud;

import com.backendcr.residentialcomplex.entity.enums.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoSolicitudRequest(
        @NotNull EstadoSolicitud estado
) {}
