package com.backendcr.residentialcomplex.dto.anuncio;

import com.backendcr.residentialcomplex.entity.Anuncio;
import com.backendcr.residentialcomplex.entity.enums.EstadoAnuncio;

public record AnuncioResponse(
        Long id,
        String titulo,
        String contenido,
        EstadoAnuncio estado,
        Long creadoPor,
        String creadoPorNombre,
        String fechaInicio,
        String fechaFin,
        String creadoEn,
        long totalVistas,
        boolean vistoPorMi   // solo relevante en el contexto residente
) {
    public static AnuncioResponse from(Anuncio a, String creadoPorNombre, long totalVistas, boolean vistoPorMi) {
        return new AnuncioResponse(
                a.getId(),
                a.getTitulo(),
                a.getContenido(),
                a.getEstado(),
                a.getCreadoPor(),
                creadoPorNombre,
                a.getFechaInicio() != null ? a.getFechaInicio().toString() : null,
                a.getFechaFin() != null ? a.getFechaFin().toString() : null,
                a.getCreadoEn() != null ? a.getCreadoEn().toString() : null,
                totalVistas,
                vistoPorMi
        );
    }
}
