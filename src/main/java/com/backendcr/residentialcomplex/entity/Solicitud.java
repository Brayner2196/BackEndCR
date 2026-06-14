package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.EstadoSolicitud;
import com.backendcr.residentialcomplex.entity.enums.TipoSolicitud;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Solicitud de compra/pedido que un residente (comprador) hace
 * sobre la publicación de otro residente (vendedor).
 */
@Entity
@Table(name = "solicitudes")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Publicación ───────────────────────────────────────
    @Column(name = "publicacion_id", nullable = false)
    private Long publicacionId;

    /** Título denormalizado para mostrar sin joins. */
    @Column(name = "publicacion_titulo", nullable = false, length = 120)
    private String publicacionTitulo;

    /** Precio unitario en el momento de la solicitud. */
    @Column(name = "publicacion_precio", nullable = false, precision = 12, scale = 0)
    private BigDecimal publicacionPrecio;

    // ── Comprador ─────────────────────────────────────────
    @Column(name = "comprador_id", nullable = false)
    private Long compradorId;

    @Column(name = "comprador_nombre", nullable = false, length = 150)
    private String compradorNombre;

    // ── Vendedor ──────────────────────────────────────────
    @Column(name = "vendedor_id", nullable = false)
    private Long vendedorId;

    @Column(name = "vendedor_nombre", nullable = false, length = 150)
    private String vendedorNombre;

    // ── Detalle del pedido ────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSolicitud tipo;

    @Column(nullable = false)
    private int cantidad = 1;

    @Column(length = 300)
    private String notas;

    // ── Estado ───────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    // ── Timestamps ───────────────────────────────────────
    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    // ── Getters / Setters ─────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPublicacionId() { return publicacionId; }
    public void setPublicacionId(Long publicacionId) { this.publicacionId = publicacionId; }

    public String getPublicacionTitulo() { return publicacionTitulo; }
    public void setPublicacionTitulo(String publicacionTitulo) { this.publicacionTitulo = publicacionTitulo; }

    public BigDecimal getPublicacionPrecio() { return publicacionPrecio; }
    public void setPublicacionPrecio(BigDecimal publicacionPrecio) { this.publicacionPrecio = publicacionPrecio; }

    public Long getCompradorId() { return compradorId; }
    public void setCompradorId(Long compradorId) { this.compradorId = compradorId; }

    public String getCompradorNombre() { return compradorNombre; }
    public void setCompradorNombre(String compradorNombre) { this.compradorNombre = compradorNombre; }

    public Long getVendedorId() { return vendedorId; }
    public void setVendedorId(Long vendedorId) { this.vendedorId = vendedorId; }

    public String getVendedorNombre() { return vendedorNombre; }
    public void setVendedorNombre(String vendedorNombre) { this.vendedorNombre = vendedorNombre; }

    public TipoSolicitud getTipo() { return tipo; }
    public void setTipo(TipoSolicitud tipo) { this.tipo = tipo; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }

    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
