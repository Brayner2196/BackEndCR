package com.backendcr.residentialcomplex.dto.consejo;

import java.util.Map;

/**
 * Respuesta del endpoint GET /api/consejo/estadisticas.
 * Agrega métricas de PQRs, Anuncios y Votaciones en un rango de fechas.
 */
public record ConsejoEstadisticasResponse(

        // ── Rango consultado ─────────────────────────────────────────────────
        String desde,
        String hasta,

        // ── PQRs ─────────────────────────────────────────────────────────────
        long pqrTotal,
        Map<String, Long> pqrPorEstado,          // RADICADA, EN_PROCESO, RESUELTO, CERRADO, RECHAZADA
        Map<String, Long> pqrPorTipo,            // PETICION, QUEJA, RECLAMO
        long pqrResueltas,                       // estado RESUELTO + CERRADO
        Double pqrTiempoPromRespuestaHoras,      // null si no hay ninguna resuelta

        // ── Anuncios ─────────────────────────────────────────────────────────
        long anuncioTotal,
        long anuncioActivos,
        long anuncioTotalVistas,                 // lecturas únicas (anuncio_vistas)

        // ── Votaciones ───────────────────────────────────────────────────────
        long votacionTotal,
        Map<String, Long> votacionPorEstado,     // BORRADOR, ABIERTA, CERRADA
        long votacionParticipantes               // residentes distintos que votaron
) {}
