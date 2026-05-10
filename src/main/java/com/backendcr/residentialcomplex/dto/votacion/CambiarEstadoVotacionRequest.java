package com.backendcr.residentialcomplex.dto.votacion;

import com.backendcr.residentialcomplex.entity.enums.EstadoVotacion;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoVotacionRequest(@NotNull EstadoVotacion estado) {}
