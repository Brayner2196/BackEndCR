package com.backendcr.residentialcomplex.dto.parqueadero;

import com.backendcr.residentialcomplex.entity.enums.ModeloParqueaderoPrivado;
import jakarta.validation.constraints.Min;

public record ConfiguracionParqueaderoRequest(
        @Min(0) int totalParqueaderos,
        @Min(0) int parqueaderosComunes,
        @Min(0) int parqueaderosPrivados,
        @Min(1) int maxVehiculosPorPropiedad,
        boolean permiteCarro,
        boolean permiteMoto,
        boolean permiteBicicleta,
        boolean requiereAprobacionVehiculo,
        /** Modelo aplicado al crear parqueaderos privados en bulk. Null → usa ACCESORIO. */
        ModeloParqueaderoPrivado modeloPrivadoDefault
) {}
