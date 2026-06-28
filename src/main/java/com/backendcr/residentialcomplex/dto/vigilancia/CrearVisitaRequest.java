package com.backendcr.residentialcomplex.dto.vigilancia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud del residente para pre-registrar una visita. Genera el código QR.
 */
public record CrearVisitaRequest(
        @NotNull Long propiedadId,
        @NotBlank @Size(max = 120) String nombreVisitante,
        @Size(max = 30) String documento,
        @Size(max = 15) String placa,
        @Size(max = 200) String motivo,
        /** Cantidad de personas que ingresan (mínimo 1). */
        Integer cantidadPersonas,
        /** Nombres de acompañantes en texto libre (opcional). */
        @Size(max = 500) String acompanantes,
        /** Inicio del horario esperado de la visita en ISO-8601 (opcional). */
        String franjaDesde,
        /** Fin del horario esperado de la visita en ISO-8601 (opcional). */
        String franjaHasta,
        /** Horas de validez del QR; si es null usa el horario o la parametrización. */
        Integer validezHoras
) {}
