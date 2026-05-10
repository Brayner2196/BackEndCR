package com.backendcr.residentialcomplex.dto.anuncio;

import com.backendcr.residentialcomplex.entity.enums.EstadoAnuncio;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoAnuncioRequest(@NotNull EstadoAnuncio estado) {}
