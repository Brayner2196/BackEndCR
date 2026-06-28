package com.backendcr.residentialcomplex.dto.vigilancia;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registro manual por el vigilante de un ingreso peatonal (visitante sin
 * pre-registro). La propiedad de destino es obligatoria; el documento puede ser
 * obligatorio según {@code ConfigVigilancia.exigeDocumentoPeatonal}.
 */
public record RegistrarAccesoPeatonalRequest(
        @NotNull Long propiedadId,
        @Size(max = 120) String nombreVisitante,
        @Size(max = 30) String documento,
        @Size(max = 200) String motivo
) {}
