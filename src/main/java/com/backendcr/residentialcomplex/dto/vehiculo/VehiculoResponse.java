package com.backendcr.residentialcomplex.dto.vehiculo;

import com.backendcr.residentialcomplex.entity.enums.EstadoVehiculo;
import com.backendcr.residentialcomplex.entity.enums.TipoVehiculo;

public record VehiculoResponse(
        Long id,
        String placa,
        TipoVehiculo tipo,
        String marca,
        String modelo,
        String color,
        Long propiedadId,
        Long parqueaderoId,
        String parqueaderoIdentificador,
        EstadoVehiculo estado,
        String motivoRechazo
) {}
