package com.backendcr.residentialcomplex.dto.vigilancia;

import com.backendcr.residentialcomplex.entity.enums.EstadoVisita;

import java.time.Instant;

/**
 * Detalle de una visita para el vigilante al escanear el QR. Incluye el estado
 * de cartera de la unidad y si la decisión de Aprobar está permitida según la
 * parametrización del conjunto.
 */
public record DetalleVisitaResponse(
        Long id,
        String codigo,
        EstadoVisita estado,
        String nombreVisitante,
        int cantidadPersonas,
        String acompanantes,
        String documento,
        String placa,
        String motivo,
        Long propiedadId,
        String propiedadIdentificador,
        Instant franjaDesde,
        Instant franjaHasta,
        Instant expiraEn,
        Instant ingresoEn,
        String motivoRechazo,
        boolean carteraRestringida,
        String carteraMensaje,
        boolean puedeDecidir,
        boolean puedeAprobar,
        String mensaje
) {}
