package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Presupuesto anual del conjunto residencial.
 * Puede haber uno por año. El admin crea y gestiona las categorías
 * y registra los gastos; los residentes ven el dashboard de ejecución.
 */
@Entity
@Table(name = "presupuestos")
public class Presupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Año al que corresponde este presupuesto (ej: 2025) */
    @Column(name = "anio", nullable = false)
    private int anio;

    /** Título o descripción opcional (ej: "Presupuesto ordinario 2025") */
    @Column(name = "titulo", length = 150)
    private String titulo;

    /**
     * Monto total presupuestado = suma de los montos asignados a cada categoría.
     * Se actualiza cuando se crean/editan categorías.
     */
    @Column(name = "monto_total_presupuestado", precision = 15, scale = 0)
    private BigDecimal montoTotalPresupuestado = BigDecimal.ZERO;

    /** true = presupuesto vigente / activo para el tenant */
    @Column(name = "activo", nullable = false)
    private boolean activo = false;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public BigDecimal getMontoTotalPresupuestado() { return montoTotalPresupuestado; }
    public void setMontoTotalPresupuestado(BigDecimal montoTotalPresupuestado) {
        this.montoTotalPresupuestado = montoTotalPresupuestado;
    }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
