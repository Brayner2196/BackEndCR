package com.backendcr.residentialcomplex.dto.presupuesto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CategoriaPresupuestoRequest(

        @NotBlank @Size(max = 100)
        String nombre,

        @Size(max = 300)
        String descripcion,

        @NotNull @DecimalMin("0.00")
        BigDecimal montoAsignado,

        /** Color hex opcional para la UI, ej: "#4CAF50" */
        @Size(max = 10)
        String color,

        /** Nombre del ícono Material, ej: "build_outlined" */
        @Size(max = 80)
        String icono
) {}
