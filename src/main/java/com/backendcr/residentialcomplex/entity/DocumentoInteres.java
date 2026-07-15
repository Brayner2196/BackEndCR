package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.CategoriaDocumento;
import com.backendcr.residentialcomplex.entity.enums.EstadoDocumento;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Documento de interés general del conjunto (reglamentos, actas, comunicados, etc.).
 *
 * El administrador lo gestiona; los residentes lo consultan cuando está PUBLICADO.
 * Cada documento agrupa una lista de archivos adjuntos ({@link ArchivoDocumento}),
 * cuyas rutas físicas viven en el bucket S3 (aquí solo se guardan las keys).
 */
@Entity
@Table(name = "documentos_interes")
public class DocumentoInteres {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 2000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaDocumento categoria = CategoriaDocumento.OTROS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDocumento estado = EstadoDocumento.BORRADOR;

    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    /** Archivos adjuntos del documento (campo tipo lista solicitado). */
    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("creadoEn ASC")
    private List<ArchivoDocumento> archivos = new ArrayList<>();

    // ─── Helpers de relación (mantienen ambos lados sincronizados) ───────────

    public void agregarArchivo(ArchivoDocumento archivo) {
        archivo.setDocumento(this);
        this.archivos.add(archivo);
    }

    public void quitarArchivo(ArchivoDocumento archivo) {
        this.archivos.remove(archivo);
        archivo.setDocumento(null);
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public CategoriaDocumento getCategoria() { return categoria; }
    public void setCategoria(CategoriaDocumento categoria) { this.categoria = categoria; }

    public EstadoDocumento getEstado() { return estado; }
    public void setEstado(EstadoDocumento estado) { this.estado = estado; }

    public Long getCreadoPor() { return creadoPor; }
    public void setCreadoPor(Long creadoPor) { this.creadoPor = creadoPor; }

    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }

    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant actualizadoEn) { this.actualizadoEn = actualizadoEn; }

    public List<ArchivoDocumento> getArchivos() { return archivos; }
    public void setArchivos(List<ArchivoDocumento> archivos) { this.archivos = archivos; }
}
