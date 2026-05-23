package com.backendcr.residentialcomplex.dto.planpago;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SolicitarPlanRequest(
        /** IDs de los cobros que el residente quiere incluir en el plan */
        @NotEmpty
        List<Long> cobrosIds,

        /** Número de cuotas deseadas */
        @NotNull @Min(1)
        Integer numeroCuotas,

        /** Observaciones opcionales del residente */
        @Size(max = 500)
        String observaciones
) {}
