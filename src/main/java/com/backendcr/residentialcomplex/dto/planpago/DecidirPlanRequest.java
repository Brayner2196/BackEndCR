package com.backendcr.residentialcomplex.dto.planpago;

import jakarta.validation.constraints.Size;

public record DecidirPlanRequest(
        /** true = aprobar, false = rechazar */
        boolean aprobar,

        /** Motivo del rechazo (requerido si aprobar=false) */
        @Size(max = 300)
        String motivoRechazo,

        /** Nota opcional del admin */
        @Size(max = 300)
        String notaAdmin
) {}
