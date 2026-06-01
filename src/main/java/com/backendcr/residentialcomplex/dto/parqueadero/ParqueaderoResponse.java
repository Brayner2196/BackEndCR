package com.backendcr.residentialcomplex.dto.parqueadero;

import com.backendcr.residentialcomplex.entity.enums.TipoParqueadero;

public record ParqueaderoResponse(
        Long id,
        String identificador,
        TipoParqueadero tipo,
        Long propiedadId,
        String propiedadIdentificador,
        Long vehiculoId,
        String vehiculoPlaca,
        String vehiculoTipo
) {}
