package com.backendcr.residentialcomplex.dto.parqueadero;

import jakarta.validation.constraints.Min;

public record ConfiguracionParqueaderoRequest(
        @Min(0) int totalParqueaderos,
        @Min(0) int parqueaderosComunes,
        @Min(0) int parqueaderosPrivados,
        @Min(1) int maxVehiculosPorPropiedad,
        boolean permiteCarro,
        boolean permiteMoto,
        boolean permiteBicicleta,
        boolean requiereAprobacionVehiculo
) {}
