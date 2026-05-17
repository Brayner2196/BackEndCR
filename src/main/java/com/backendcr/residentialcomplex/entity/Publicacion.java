package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.converter.StringListConverter;
import com.backendcr.residentialcomplex.entity.enums.CategoriaPublicacion;
import com.backendcr.residentialcomplex.entity.enums.EstadoPublicacion;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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

    /** Marca o fabricante del producto/servicio. */
    @Column(length = 100)
    private String marca;

    // ── Stock y logística ─────────────────────────────────
    /**
     * Unidades disponibles. null = no maneja stock | 0 = agotado | >0 = disponible.
     */
    @Column
    private Integer stock;

    /** Indica si el vendedor ofrece entrega a domicilio dentro del conjunto. */
    @Column(name = "acepta_domicilio", nullable = false)
    private boolean aceptaDomicilio = false;

    /**
     * Métodos de pago aceptados. Almacenados como texto CSV.
     * Ej: "EFECTIVO,NEQUI,TRANSFERENCIA"
     */
    @Convert(converter = StringListConverter.class)
    @Column(name = "metodos_pago", length = 300)
    private List<String> metodosPago = Collections.emptyList();

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

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public boolean isAceptaDomicilio() { return aceptaDomicilio; }
    public void setAceptaDomicilio(boolean aceptaDomicilio) { this.aceptaDomicilio = aceptaDomicilio; }

    public List<String> getMetodosPago() { return metodosPago; }
    public void setMetodosPago(List<String> metodosPago) {
        this.metodosPago = metodosPago != null ? metodosPago : Collections.emptyList();
    }

    public EstadoPublicacion getEstado() { return estado; }
    public void setEstado(EstadoPublicacion estado) { this.estado = estado; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
