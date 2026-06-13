package com.backendcr.residentialcomplex.dto.propiedad;

import java.util.List;

import com.backendcr.residentialcomplex.entity.enums.EstadoPropiedad;

public record PropiedadResponse(
        Long id,
        Long tipoId,
        String nombreTipo,
        Long parentId,
        String identificador,
        String pathTexto,
        String pathTextoCorto,
        boolean esFacturable,
        boolean esParqueadero,
        EstadoPropiedad estado,
        List<ResidenteResumenDto> residentes
) {}
