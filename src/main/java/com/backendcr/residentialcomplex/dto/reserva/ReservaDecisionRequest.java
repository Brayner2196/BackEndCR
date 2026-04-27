package com.backendcr.residentialcomplex.dto.reserva;

import jakarta.validation.constraints.Size;

public record ReservaDecisionRequest(
        @Size(max = 300) String motivo
) {}
