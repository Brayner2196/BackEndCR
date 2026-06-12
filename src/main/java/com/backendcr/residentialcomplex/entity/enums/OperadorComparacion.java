package com.backendcr.residentialcomplex.entity.enums;

import java.math.BigDecimal;

/**
 * Operador de comparación numérica usado por una condición de regla de cartera.
 * El método {@link #evaluar(BigDecimal, BigDecimal)} aplica el operador entre el
 * valor real (de las métricas) y el valor umbral configurado.
 */
public enum OperadorComparacion {
    MAYOR_QUE,
    MAYOR_IGUAL,
    MENOR_QUE,
    MENOR_IGUAL,
    IGUAL,
    DIFERENTE;

    /** @return resultado de {@code real <op> umbral}. */
    public boolean evaluar(BigDecimal real, BigDecimal umbral) {
        if (real == null || umbral == null) return false;
        int cmp = real.compareTo(umbral);
        return switch (this) {
            case MAYOR_QUE   -> cmp > 0;
            case MAYOR_IGUAL -> cmp >= 0;
            case MENOR_QUE   -> cmp < 0;
            case MENOR_IGUAL -> cmp <= 0;
            case IGUAL       -> cmp == 0;
            case DIFERENTE   -> cmp != 0;
        };
    }
}
