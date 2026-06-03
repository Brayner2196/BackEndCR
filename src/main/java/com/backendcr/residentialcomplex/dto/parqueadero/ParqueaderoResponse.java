package com.backendcr.residentialcomplex.dto.parqueadero;

import com.backendcr.residentialcomplex.entity.enums.ModeloParqueaderoPrivado;
import com.backendcr.residentialcomplex.entity.enums.TipoParqueadero;

public record ParqueaderoResponse(
        Long id,
        String identificador,
        TipoParqueadero tipo,
        /** INDEPENDIENTE o ACCESORIO (null si es COMUN) */
        ModeloParqueaderoPrivado modeloPropiedad,
        /** ACCESORIO: ID del apartamento. INDEPENDIENTE: ID de apartamento relacionado (opcional) */
        Long propiedadId,
        String propiedadIdentificador,
        /** INDEPENDIENTE: ID de la Propiedad-parqueadero en el árbol de tipos */
        Long propiedadParqueaderoId,
        Long vehiculoId,
        String vehiculoPlaca,
        String vehiculoTipo
) {}
