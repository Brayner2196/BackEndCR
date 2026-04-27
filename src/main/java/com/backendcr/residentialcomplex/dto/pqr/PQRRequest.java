package com.backendcr.residentialcomplex.dto.pqr;

import com.backendcr.residentialcomplex.entity.enums.TipoPQR;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PQRRequest(
        @NotNull TipoPQR tipo,
        @NotBlank @Size(max = 200) String asunto,
        @NotBlank @Size(max = 2000) String descripcion,
        Long propiedadId
) {}
