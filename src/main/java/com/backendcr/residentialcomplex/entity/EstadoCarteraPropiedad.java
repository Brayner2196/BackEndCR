package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Snapshot del estado de cartera vigente de una propiedad. Evita recalcular el
 * motor en cada consulta. Relación 1:1 lógica con la propiedad.
 */
@Entity
@Table(name = "estado_cartera_propiedad",
       uniqueConstraints = @UniqueConstraint(columnNames = "propiedad_id"))
public class EstadoCarteraPropiedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "propiedad_id", nullable = false)
    private Long propiedadId;

    @Column(name = "estado_cartera_id", nullable = false)
    private Long estadoCarteraId;

    @Column(name = "dias_vencido_max", nullable = false)
    private int diasVencidoMax = 0;

    @Column(name = "monto_adeudado", precision = 16, scale = 2)
    private BigDecimal montoAdeudado = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "calculado_en", nullable = false)
    private Instant calculadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPropiedadId() { return propiedadId; }
    public void setPropiedadId(Long propiedadId) { this.propiedadId = propiedadId; }

    public Long getEstadoCarteraId() { return estadoCarteraId; }
    public void setEstadoCarteraId(Long estadoCarteraId) { this.estadoCarteraId = estadoCarteraId; }

    public int getDiasVencidoMax() { return diasVencidoMax; }
    public void setDiasVencidoMax(int diasVencidoMax) { this.diasVencidoMax = diasVencidoMax; }

    public BigDecimal getMontoAdeudado() { return montoAdeudado; }
    public void setMontoAdeudado(BigDecimal montoAdeudado) { this.montoAdeudado = montoAdeudado; }

    public Instant getCalculadoEn() { return calculadoEn; }
}
