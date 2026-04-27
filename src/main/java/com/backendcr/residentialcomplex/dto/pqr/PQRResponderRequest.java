package com.backendcr.residentialcomplex.dto.pqr;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PQRResponderRequest(
        @NotBlank @Size(max = 2000) String respuesta
) {}
