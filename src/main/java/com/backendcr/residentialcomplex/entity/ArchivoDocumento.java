package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.TipoArchivo;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Archivo adjunto de un {@link DocumentoInteres}.
 *
 * Esta tabla relaciona cada documento con las rutas (keys) de sus archivos físicos
 * en el bucket S3. El binario NO se guarda en la base de datos: solo la key y su metadata.
 */
@Entity
@Table(name = "archivos_documento")
public class ArchivoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoInteres documento;

    /** Key del objeto en el bucket S3 (ej: documentos/{tenant}/{uuid}.pdf). */
    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoArchivo tipo;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DocumentoInteres getDocumento() { return documento; }
    public void setDocumento(DocumentoInteres documento) { this.documento = documento; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public TipoArchivo getTipo() { return tipo; }
    public void setTipo(TipoArchivo tipo) { this.tipo = tipo; }

    public Long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(Long tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
}
