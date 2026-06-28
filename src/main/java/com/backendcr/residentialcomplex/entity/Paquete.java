package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.EstadoPaquete;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Correspondencia/paquete recibido en portería para una propiedad.
 */
@Entity
@Table(name = "paquetes")
public class Paquete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "propiedad_id", nullable = false)
    private Long propiedadId;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(length = 120)
    private String remitente;

    @Column(length = 80)
    private String transportadora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoPaquete estado = EstadoPaquete.RECIBIDO;

    @Column(name = "recibido_por")
    private Long recibidoPor;

    @Column(name = "entregado_en")
    private Instant entregadoEn;

    @Column(name = "entregado_a", length = 120)
    private String entregadoA;

    @Column(name = "entregado_por")
    private Long entregadoPor;

    @CreationTimestamp
    @Column(name = "recibido_en", nullable = false, updatable = false)
    private Instant recibidoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    // ── Getters / Setters ─────────────────────────────────────

    public Long getId() { return id; }

    public Long getPropiedadId() { return propiedadId; }
    public void setPropiedadId(Long propiedadId) { this.propiedadId = propiedadId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getRemitente() { return remitente; }
    public void setRemitente(String remitente) { this.remitente = remitente; }

    public String getTransportadora() { return transportadora; }
    public void setTransportadora(String transportadora) { this.transportadora = transportadora; }

    public EstadoPaquete getEstado() { return estado; }
    public void setEstado(EstadoPaquete estado) { this.estado = estado; }

    public Long getRecibidoPor() { return recibidoPor; }
    public void setRecibidoPor(Long recibidoPor) { this.recibidoPor = recibidoPor; }

    public Instant getEntregadoEn() { return entregadoEn; }
    public void setEntregadoEn(Instant entregadoEn) { this.entregadoEn = entregadoEn; }

    public String getEntregadoA() { return entregadoA; }
    public void setEntregadoA(String entregadoA) { this.entregadoA = entregadoA; }

    public Long getEntregadoPor() { return entregadoPor; }
    public void setEntregadoPor(Long entregadoPor) { this.entregadoPor = entregadoPor; }

    public Instant getRecibidoEn() { return recibidoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
