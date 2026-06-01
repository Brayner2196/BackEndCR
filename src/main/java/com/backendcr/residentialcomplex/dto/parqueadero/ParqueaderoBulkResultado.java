package com.backendcr.residentialcomplex.dto.parqueadero;

import java.util.List;

public record ParqueaderoBulkResultado(
        int creados,
        int duplicados,
        List<ItemResultado> items
) {
    public record ItemResultado(
            String identificador,
            String estado,      // "CREADO" | "DUPLICADO"
            Long id             // null si DUPLICADO
    ) {}
}
