package com.backendcr.residentialcomplex.dto.parqueadero;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// Los parqueaderos creados en bulk son siempre PRIVADOS.
// Los comunales solo existen como conteo en configuracion_parqueadero.
public record ParqueaderoBulkRequest(
        @NotNull List<Item> items
) {
    public record Item(
            @NotBlank String identificador
    ) {}
}
