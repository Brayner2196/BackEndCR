package com.backendcr.residentialcomplex.dto.votacion;

import com.backendcr.residentialcomplex.entity.VotoResidente;

public record VotoResidenteResponse(
        Long residenteId,
        String residenteNombre,
        Long opcionId,
        Integer valorNumerico,
        String respuestaTexto,
        String votadoEn
) {
    public static VotoResidenteResponse from(VotoResidente v) {
        return new VotoResidenteResponse(
                v.getResidenteId(), v.getResidenteNombre(),
                v.getOpcionId(), v.getValorNumerico(), v.getRespuestaTexto(),
                v.getVotadoEn() != null ? v.getVotadoEn().toString() : null
        );
    }
}
