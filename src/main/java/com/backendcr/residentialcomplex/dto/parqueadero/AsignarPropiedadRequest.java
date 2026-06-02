package com.backendcr.residentialcomplex.dto.parqueadero;

/**
 * Permite asignar o desasignar un parqueadero a una propiedad.
 * propiedadId = null → desasigna.
 */
public record AsignarPropiedadRequest(Long propiedadId) {}
