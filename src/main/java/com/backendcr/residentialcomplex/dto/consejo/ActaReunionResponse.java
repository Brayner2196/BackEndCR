package com.backendcr.residentialcomplex.dto.consejo;

import com.backendcr.residentialcomplex.entity.enums.EstadoActa;

import java.time.Instant;

public record ActaReunionResponse(
        Long id,
        String titulo,
        Instant fechaReunion,
        EstadoActa estado,
        String transcripcion,
        String contenido,
        Integer duracionSegundos,
        Long creadoPorUsuarioId,
        String creadoPorNombre,
        String errorMensaje,
        Instant finalizadaEn,
        Instant creadoEn,
        Instant actualizadoEn
) {}
