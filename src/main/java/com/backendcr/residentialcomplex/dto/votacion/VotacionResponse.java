package com.backendcr.residentialcomplex.dto.votacion;

import com.backendcr.residentialcomplex.entity.Votacion;
import com.backendcr.residentialcomplex.entity.enums.EstadoVotacion;
import com.backendcr.residentialcomplex.entity.enums.TipoVotacion;

import java.util.List;

public record VotacionResponse(
        Long id,
        String titulo,
        String descripcion,
        TipoVotacion tipoVotacion,
        EstadoVotacion estado,
        Integer escalaMax,
        boolean mostrarVotantes,
        boolean permiteCambiarVoto,
        boolean mostrarPorcentajes,
        String fechaInicio,
        String fechaFin,
        String creadoEn,
        Long creadoPor,
        long totalVotantes,
        boolean yaVote,                        // solo relevante en contexto residente
        List<OpcionVotacionResponse> opciones,
        List<VotoResidenteResponse> votantes,  // solo si mostrarVotantes=true o es admin

        // ── Voto propio del residente autenticado ──────────────────────────────
        List<Long> miVotoOpcionIds,            // IDs de opciones elegidas (OPCION_UNICA / OPCION_MULTIPLE)
        Integer miVotoValorNumerico,           // valor elegido (ESCALA_NUMERICA)
        String miVotoRespuestaTexto            // texto escrito (TEXTO_LIBRE)
) {
    public static VotacionResponse from(Votacion v, long totalVotantes, boolean yaVote,
                                        List<OpcionVotacionResponse> opciones,
                                        List<VotoResidenteResponse> votantes,
                                        List<Long> miVotoOpcionIds,
                                        Integer miVotoValorNumerico,
                                        String miVotoRespuestaTexto) {
        return new VotacionResponse(
                v.getId(), v.getTitulo(), v.getDescripcion(),
                v.getTipoVotacion(), v.getEstado(), v.getEscalaMax(),
                v.isMostrarVotantes(), v.isPermiteCambiarVoto(), v.isMostrarPorcentajes(),
                v.getFechaInicio() != null ? v.getFechaInicio().toString() : null,
                v.getFechaFin()    != null ? v.getFechaFin().toString()    : null,
                v.getCreadoEn()    != null ? v.getCreadoEn().toString()    : null,
                v.getCreadoPor(), totalVotantes, yaVote, opciones, votantes,
                miVotoOpcionIds, miVotoValorNumerico, miVotoRespuestaTexto
        );
    }
}
