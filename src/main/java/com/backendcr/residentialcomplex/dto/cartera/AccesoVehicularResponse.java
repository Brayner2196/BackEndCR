package com.backendcr.residentialcomplex.dto.cartera;

/**
 * Respuesta para el vigilante al validar una placa: indica si el vehículo puede
 * ingresar según el estado de cartera de su propiedad.
 */
public record AccesoVehicularResponse(
        boolean permitido,
        String placa,
        Long propiedadId,
        String propiedad,
        String estadoCodigo,
        String estadoNombre,
        String mensaje
) {}
