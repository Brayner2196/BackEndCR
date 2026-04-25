package com.backendcr.residentialcomplex.dto.pago;

import jakarta.validation.constraints.NotBlank;

public record ExonerarCobroRequest(@NotBlank String nota) {}
