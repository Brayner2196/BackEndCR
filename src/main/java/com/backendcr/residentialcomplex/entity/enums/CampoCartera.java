package com.backendcr.residentialcomplex.entity.enums;

/**
 * Campos de las métricas de cartera de una propiedad que una condición de regla
 * puede evaluar. El motor resuelve el valor de cada campo desde
 * {@code MetricasCartera} en tiempo de evaluación.
 */
public enum CampoCartera {
    /** Mayor número de días vencidos entre los cobros de la propiedad. */
    DIAS_VENCIDO_MAX,
    /** Suma del saldo pendiente de la propiedad. */
    MONTO_ADEUDADO,
    /** Cantidad de periodos distintos con cobro vencido. */
    NUM_PERIODOS_VENCIDOS,
    /** Cantidad de cobros vencidos con saldo pendiente. */
    NUM_COBROS_VENCIDOS
}
