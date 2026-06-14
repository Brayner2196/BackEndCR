package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Configuración global del módulo de planes de pago para el tenant.
 * Solo existe un registro por tenant (upsert).
 */
@Entity
@Table(name = "configuracion_plan_pago")
public class ConfiguracionPlanPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Módulo habilitado/deshabilitado */
    private boolean activo = false;

    /** Número máximo de cuotas permitidas (ej: 3, 6, 12) */
    @Column(name = "max_cuotas", nullable = false)
    private int maxCuotas = 3;

    /** Si aplica un recargo por fraccionar el pago */
    @Column(name = "recargo_fraccionamiento", nullable = false)
    private boolean recargoFraccionamiento = false;

    /** Porcentaje de recargo sobre el total de la deuda (ej: 5.00 = 5%) */
    @Column(name = "porcentaje_recargo", precision = 5, scale = 2)
    private BigDecimal porcentajeRecargo = BigDecimal.ZERO;

    /**
     * true  = la mora se congela mientras el plan esté activo
     * false = la mora continúa acumulando durante el plan
     */
    @Column(name = "mora_congelada_durante_plan", nullable = false)
    private boolean moraCongeladaDurantePlan = false;

    /**
     * true  = plan se aprueba automáticamente si cumple las reglas
     * false = admin debe aprobar manualmente
     */
    @Column(name = "aprobacion_automatica", nullable = false)
    private boolean aprobacionAutomatica = false;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    // ── Getters / Setters ──────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public int getMaxCuotas() { return maxCuotas; }
    public void setMaxCuotas(int maxCuotas) { this.maxCuotas = maxCuotas; }

    public boolean isRecargoFraccionamiento() { return recargoFraccionamiento; }
    public void setRecargoFraccionamiento(boolean recargoFraccionamiento) {
        this.recargoFraccionamiento = recargoFraccionamiento;
    }

    public BigDecimal getPorcentajeRecargo() { return porcentajeRecargo; }
    public void setPorcentajeRecargo(BigDecimal porcentajeRecargo) {
        this.porcentajeRecargo = porcentajeRecargo;
    }

    public boolean isMoraCongeladaDurantePlan() { return moraCongeladaDurantePlan; }
    public void setMoraCongeladaDurantePlan(boolean moraCongeladaDurantePlan) {
        this.moraCongeladaDurantePlan = moraCongeladaDurantePlan;
    }

    public boolean isAprobacionAutomatica() { return aprobacionAutomatica; }
    public void setAprobacionAutomatica(boolean aprobacionAutomatica) {
        this.aprobacionAutomatica = aprobacionAutomatica;
    }

    public Instant getActualizadoEn() { return actualizadoEn; }
}
