package com.backendcr.residentialcomplex.dto.publicacion;

import com.backendcr.residentialcomplex.entity.enums.CategoriaPublicacion;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record PublicacionRequest(
        @NotBlank @Size(max = 120)  String titulo,
        @Size(max = 1000)           String descripcion,
        @NotNull @DecimalMin("0.0") BigDecimal precio,
        @NotNull                    CategoriaPublicacion categoria,
        @Size(max = 200)            String contacto,
        @Size(max = 100)            String marca,

        /** null = sin control de stock, 0 = agotado, >0 = unidades disponibles. */
        @Min(0)                     Integer stock,

        boolean                     aceptaDomicilio,

        /** Métodos de pago aceptados (máx 10 valores, cada uno máx 30 chars). */
        @Size(max = 10)             List<@Size(max = 30) String> metodosPago,

        /**
         * ID de la propiedad del vendedor enviado desde el cliente al crear.
         * null en ediciones (el backend mantiene el valor original).
         */
                                    Long propiedadId
) {}
