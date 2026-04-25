package com.backendcr.residentialcomplex.dto.pago;

import jakarta.validation.constraints.NotBlank;

public record RechazarPagoRequest(@NotBlank String motivoRechazo) {}
