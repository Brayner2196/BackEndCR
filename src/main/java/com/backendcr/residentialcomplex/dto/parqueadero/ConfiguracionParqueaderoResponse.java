package com.backendcr.residentialcomplex.dto.parqueadero;

public record ConfiguracionParqueaderoResponse(
        Long id,
        int totalParqueaderos,
        int parqueaderosComunes,
        int parqueaderosPrivados,
        int maxVehiculosPorPropiedad,
        boolean permiteCarro,
        boolean permiteMoto,
        boolean permiteBicicleta,
        boolean requiereAprobacionVehiculo
) {}
