package com.backendcr.residentialcomplex.dto.votacion;

import java.util.List;

/**
 * Payload para registrar o actualizar el voto de un residente.
 * Según el tipo de votación, solo uno de estos campos debe tener valor.
 */
public record RegistrarVotoRequest(
        List<Long> opcionIds,     // OPCION_UNICA (1 elemento) o OPCION_MULTIPLE (N elementos)
        Integer valorNumerico,    // ESCALA_NUMERICA
        String respuestaTexto     // TEXTO_LIBRE
) {}
