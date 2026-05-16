package com.backendcr.residentialcomplex.dto.publicacion;

import com.backendcr.residentialcomplex.entity.enums.CategoriaPublicacion;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PublicacionRequest(
        @NotBlank @Size(max = 120) String titulo,
        @Size(max = 1000) String descripcion,
        @NotNull @DecimalMin("0.0") BigDecimal precio,
        @NotNull CategoriaPublicacion categoria,
        @Size(max = 200) String contacto
) {}
