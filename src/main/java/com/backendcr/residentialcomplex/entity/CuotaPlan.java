package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.EstadoCuotaPlan;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cuota individual dentro de un PlanPago.
 */
@Entity
@Table(name = "cuotas_plan")
public class CuotaPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "numero_cuota", nullable = false)
    private int numeroCuota;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal monto;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuotaPlan estado = EstadoCuotaPlan.PENDIENTE;

    /** Fecha en que se registró el pago de esta cuota */
    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    /** Observación del admin al marcar como pagada */
    @Column(name = "nota_pago", length = 200)
    private String notaPago;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    // ── Getters / Setters ──────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public int getNumeroCuota() { return numeroCuota; }
    public void setNumeroCuota(int numeroCuota) { this.numeroCuota = numeroCuota; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public EstadoCuotaPlan getEstado() { return estado; }
    public void setEstado(EstadoCuotaPlan estado) { this.estado = estado; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public String getNotaPago() { return notaPago; }
    public void setNotaPago(String notaPago) { this.notaPago = notaPago; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
