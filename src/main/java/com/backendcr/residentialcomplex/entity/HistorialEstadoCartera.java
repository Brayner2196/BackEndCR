package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Auditoría: una fila por cada transición de estado de cartera de una propiedad.
 */
@Entity
@Table(name = "historial_estado_cartera")
public class HistorialEstadoCartera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "propiedad_id", nullable = false)
    private Long propiedadId;

    /** Null si es la primera asignación. */
    @Column(name = "estado_anterior_id")
    private Long estadoAnteriorId;

    @Column(name = "estado_nuevo_id", nullable = false)
    private Long estadoNuevoId;

    @Column(name = "dias_vencido_max", nullable = false)
    private int diasVencidoMax;

    @Column(name = "monto_adeudado", precision = 16, scale = 2)
    private BigDecimal montoAdeudado;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPropiedadId() { return propiedadId; }
    public void setPropiedadId(Long propiedadId) { this.propiedadId = propiedadId; }

    public Long getEstadoAnteriorId() { return estadoAnteriorId; }
    public void setEstadoAnteriorId(Long estadoAnteriorId) { this.estadoAnteriorId = estadoAnteriorId; }

    public Long getEstadoNuevoId() { return estadoNuevoId; }
    public void setEstadoNuevoId(Long estadoNuevoId) { this.estadoNuevoId = estadoNuevoId; }

    public int getDiasVencidoMax() { return diasVencidoMax; }
    public void setDiasVencidoMax(int diasVencidoMax) { this.diasVencidoMax = diasVencidoMax; }

    public BigDecimal getMontoAdeudado() { return montoAdeudado; }
    public void setMontoAdeudado(BigDecimal montoAdeudado) { this.montoAdeudado = montoAdeudado; }

    public Instant getCreadoEn() { return creadoEn; }
}
