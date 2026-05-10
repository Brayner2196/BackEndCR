package com.backendcr.residentialcomplex.dto.anuncio;

import com.backendcr.residentialcomplex.entity.AnuncioVista;

public record AnuncioVistaResponse(
        Long residenteId,
        String residenteNombre,
        String vistoEn
) {
    public static AnuncioVistaResponse from(AnuncioVista v) {
        return new AnuncioVistaResponse(v.getResidenteId(), v.getResidenteNombre(),
                v.getVistoEn() != null ? v.getVistoEn().toString() : null);
    }
}
