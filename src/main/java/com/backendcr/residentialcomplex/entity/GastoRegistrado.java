package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Gasto individual registrado por el admin contra una categoría del presupuesto.
 */
@Entity
@Table(name = "gastos_registrados")
public class GastoRegistrado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "presupuesto_id", nullable = false)
    private Long presupuestoId;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    @Column(name = "descripcion", nullable = false, length = 300)
    private String descripcion;

    @Column(name = "monto", nullable = false, precision = 15, scale = 0)
    private BigDecimal monto;

    /** Fecha en que ocurrió el gasto (no necesariamente cuando se registró) */
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    /** URL o referencia de comprobante/factura. Opcional. */
    @Column(name = "comprobante", length = 500)
    private String comprobante;

    /** Usuario administrador que registró el gasto */
    @Column(name = "registrado_por", nullable = false)
    private Long registradoPor;

    @CreationTimestamp
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPresupuestoId() { return presupuestoId; }
    public void setPresupuestoId(Long presupuestoId) { this.presupuestoId = presupuestoId; }

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getComprobante() { return comprobante; }
    public void setComprobante(String comprobante) { this.comprobante = comprobante; }

    public Long getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(Long registradoPor) { this.registradoPor = registradoPor; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
}
