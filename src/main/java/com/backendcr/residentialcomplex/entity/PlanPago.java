package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.EstadoPlan;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Solicitud / plan de pago fraccionado creado por un residente.
 */
@Entity
@Table(name = "planes_pago")
public class PlanPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "propiedad_id", nullable = false)
    private Long propiedadId;

    @Column(name = "residente_id", nullable = false)
    private Long residenteId;

    /** Suma de la deuda total que se está fraccionando */
    @Column(name = "monto_total_deuda", nullable = false, precision = 12, scale = 0)
    private BigDecimal montoTotalDeuda;

    /** Número de cuotas elegidas por el residente */
    @Column(name = "numero_cuotas", nullable = false)
    private int numeroCuotas;

    /** Recargo aplicado por fraccionamiento (0 si no aplica) */
    @Column(name = "monto_recargo", precision = 12, scale = 0)
    private BigDecimal montoRecargo = BigDecimal.ZERO;

    /** montoTotalDeuda + montoRecargo */
    @Column(name = "monto_total_plan", nullable = false, precision = 12, scale = 0)
    private BigDecimal montoTotalPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPlan estado = EstadoPlan.PENDIENTE;

    /**
     * IDs de los cobros incluidos en el plan, separados por coma.
     * Ej: "12,13,14"
     */
    @Column(name = "cobros_incluidos", length = 500)
    private String cobrosIncluidos;

    /** Observaciones del residente al solicitar */
    @Column(length = 500)
    private String observaciones;

    /** Motivo del rechazo (solo si estado = RECHAZADO) */
    @Column(name = "motivo_rechazo", length = 300)
    private String motivoRechazo;

    /** Notas del admin al aprobar/rechazar */
    @Column(name = "nota_admin", length = 300)
    private String notaAdmin;

    @Column(name = "aprobado_por")
    private Long aprobadoPor;

    @Column(name = "fecha_decision")
    private LocalDateTime fechaDecision;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    // ── Getters / Setters ──────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPropiedadId() { return propiedadId; }
    public void setPropiedadId(Long propiedadId) { this.propiedadId = propiedadId; }

    public Long getResidenteId() { return residenteId; }
    public void setResidenteId(Long residenteId) { this.residenteId = residenteId; }

    public BigDecimal getMontoTotalDeuda() { return montoTotalDeuda; }
    public void setMontoTotalDeuda(BigDecimal montoTotalDeuda) { this.montoTotalDeuda = montoTotalDeuda; }

    public int getNumeroCuotas() { return numeroCuotas; }
    public void setNumeroCuotas(int numeroCuotas) { this.numeroCuotas = numeroCuotas; }

    public BigDecimal getMontoRecargo() { return montoRecargo; }
    public void setMontoRecargo(BigDecimal montoRecargo) { this.montoRecargo = montoRecargo; }

    public BigDecimal getMontoTotalPlan() { return montoTotalPlan; }
    public void setMontoTotalPlan(BigDecimal montoTotalPlan) { this.montoTotalPlan = montoTotalPlan; }

    public EstadoPlan getEstado() { return estado; }
    public void setEstado(EstadoPlan estado) { this.estado = estado; }

    public String getCobrosIncluidos() { return cobrosIncluidos; }
    public void setCobrosIncluidos(String cobrosIncluidos) { this.cobrosIncluidos = cobrosIncluidos; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }

    public String getNotaAdmin() { return notaAdmin; }
    public void setNotaAdmin(String notaAdmin) { this.notaAdmin = notaAdmin; }

    public Long getAprobadoPor() { return aprobadoPor; }
    public void setAprobadoPor(Long aprobadoPor) { this.aprobadoPor = aprobadoPor; }

    public LocalDateTime getFechaDecision() { return fechaDecision; }
    public void setFechaDecision(LocalDateTime fechaDecision) { this.fechaDecision = fechaDecision; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
