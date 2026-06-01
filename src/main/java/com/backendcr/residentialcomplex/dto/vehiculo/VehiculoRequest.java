package com.backendcr.residentialcomplex.dto.vehiculo;

import com.backendcr.residentialcomplex.entity.enums.TipoVehiculo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehiculoRequest(
        @NotBlank String placa,
        @NotNull TipoVehiculo tipo,
        String marca,
        String modelo,
        String color
) {}
