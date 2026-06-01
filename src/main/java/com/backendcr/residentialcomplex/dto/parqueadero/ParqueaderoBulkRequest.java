package com.backendcr.residentialcomplex.dto.parqueadero;

import com.backendcr.residentialcomplex.entity.enums.TipoParqueadero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ParqueaderoBulkRequest(
        @NotNull List<Item> items
) {
    public record Item(
            @NotBlank String identificador,
            @NotNull TipoParqueadero tipo
    ) {}
}
