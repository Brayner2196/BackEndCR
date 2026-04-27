package com.backendcr.residentialcomplex.dto.pqr;

import com.backendcr.residentialcomplex.entity.enums.EstadoPQR;
import jakarta.validation.constraints.NotNull;

public record PQRCambiarEstadoRequest(
        @NotNull EstadoPQR estado
) {}
