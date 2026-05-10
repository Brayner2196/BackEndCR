package com.backendcr.residentialcomplex.dto.votacion;

import com.backendcr.residentialcomplex.entity.OpcionVotacion;

public record OpcionVotacionResponse(
        Long id,
        String texto,
        int orden,
        long totalVotos  // para el admin o cuando la votación esté cerrada
) {
    public static OpcionVotacionResponse from(OpcionVotacion o, long totalVotos) {
        return new OpcionVotacionResponse(o.getId(), o.getTexto(), o.getOrden(), totalVotos);
    }
}
