package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Registra cómo se distribuyó cada abono entre los cobros (lógica FIFO).
 * - abonoId puede ser null cuando el movimiento proviene de saldo a favor previo.
 * - cobroId puede ser null cuando el remanente se destina a saldo a favor.
 */
@Entity
@Table(name = "movimientos_abono")
public class MovimientoAbono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "abono_id")
    private Long abonoId;

    @Column(name = "cobro_id")
    private Long cobroId;

    @Column(name = "monto_aplicado", nullable = false, precision = 12, scale = 0)
    private BigDecimal montoAplicado;

    @Column(length = 100)
    private String descripcion;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAbonoId() { return abonoId; }
    public void setAbonoId(Long abonoId) { this.abonoId = abonoId; }
    public Long getCobroId() { return cobroId; }
    public void setCobroId(Long cobroId) { this.cobroId = cobroId; }
    public BigDecimal getMontoAplicado() { return montoAplicado; }
    public void setMontoAplicado(BigDecimal montoAplicado) { this.montoAplicado = montoAplicado; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Instant getCreadoEn() { return creadoEn; }
}
