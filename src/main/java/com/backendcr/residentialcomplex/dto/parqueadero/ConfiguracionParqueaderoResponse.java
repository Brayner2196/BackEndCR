package com.backendcr.residentialcomplex.dto.parqueadero;

import com.backendcr.residentialcomplex.entity.enums.ModeloParqueaderoPrivado;

public record ConfiguracionParqueaderoResponse(
        Long id,
        int totalParqueaderos,
        int parqueaderosComunes,
        int parqueaderosPrivados,
        int maxVehiculosPorPropiedad,
        boolean permiteCarro,
        boolean permiteMoto,
        boolean permiteBicicleta,
        boolean requiereAprobacionVehiculo,
        ModeloParqueaderoPrivado modeloPrivadoDefault,
        boolean aceptaParqueaderoVisitantes,
        int totalParqueaderosVisitantes
) {}
