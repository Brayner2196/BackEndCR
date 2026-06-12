package com.backendcr.residentialcomplex.dto.cartera;

import com.backendcr.residentialcomplex.entity.enums.AccionRestringible;

/** Acción que un estado bloquea, con su mensaje. */
public record RestriccionDto(
        AccionRestringible accion,
        String mensaje
) {}
