package com.backendcr.residentialcomplex.dto.votacion;

import com.backendcr.residentialcomplex.entity.enums.TipoVotacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VotacionRequest(
        @NotBlank @Size(max = 300) String titulo,
        @Size(max = 2000) String descripcion,
        @NotNull TipoVotacion tipoVotacion,
        Integer escalaMax,
        boolean mostrarVotantes,
        boolean permiteCambiarVoto,
        boolean mostrarPorcentajes,
        String fechaInicio,
        String fechaFin,
        List<String> opciones  // solo para OPCION_UNICA y OPCION_MULTIPLE
) {}
