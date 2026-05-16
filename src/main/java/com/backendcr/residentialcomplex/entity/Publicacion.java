package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.CategoriaPublicacion;
import com.backendcr.residentialcomplex.entity.enums.EstadoPublicacion;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "publicaciones")
public class Publicacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Vendedor ────────────────────────────────────────
    @Column(name = "vendedor_id", nullable = false)
    private Long vendedorId;

    /** Nombre denormalizado para evitar joins en listados. */
    @Column(name = "vendedor_nombre", nullable = false, length = 150)
    private String vendedorNombre;

    /** Propiedad principal del vendedor: usada para calcular proximidad. */
    @Column(name = "propiedad_id")
    private Long propiedadId;

    // ── Contenido ────────────────────────────────────────
    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaPublicacion categoria;

    /** Dato de contacto que el vendedor quiere mostrar (teléfono, nota, etc.) */
    @Column(length = 200)
    private String contacto;

    // ── Estado ───────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPublicacion estado = EstadoPublicacion.ACTIVA;

    // ── Timestamps ───────────────────────────────────────
    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    // ── Getters / Setters ─────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVendedorId() { return vendedorId; }
    public void setVendedorId(Long vendedorId) { this.vendedorId = vendedorId; }

    public String getVendedorNombre() { return vendedorNombre; }
    public void setVendedorNombre(String vendedorNombre) { this.vendedorNombre = vendedorNombre; }

    public Long getPropiedadId() { return propiedadId; }
    public void setPropiedadId(Long propiedadId) { this.propiedadId = propiedadId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public CategoriaPublicacion getCategoria() { return categoria; }
    public void setCategoria(CategoriaPublicacion categoria) { this.categoria = categoria; }

    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }

    public EstadoPublicacion getEstado() { return estado; }
    public void setEstado(EstadoPublicacion estado) { this.estado = estado; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
