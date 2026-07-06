package com.backendcr.residentialcomplex.entity.enums;

/**
 * Ciclo de vida de un acta de reunión generada por voz (Whisper).
 *
 * PROCESANDO → el audio fue subido y está en cola/transcribiéndose.
 * BORRADOR   → transcripción lista; el presidente puede editar el contenido.
 * FINALIZADA → acta cerrada; inmutable.
 * ERROR      → la transcripción falló; se puede reintentar.
 */
public enum EstadoActa {
    PROCESANDO,
    BORRADOR,
    FINALIZADA,
    ERROR
}
