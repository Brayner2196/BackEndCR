package com.backendcr.residentialcomplex.dto.presupuesto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record PresupuestoRequest(

        @NotNull @Min(2000) @Max(2100)
        Integer anio,

        @Size(max = 150)
        String titulo,

        /** Si se pasa true, este presupuesto se marcará como activo (desactiva los demás) */
        boolean activo,

        @NotEmpty @Valid
        List<CategoriaPresupuestoRequest> categorias
) {}
