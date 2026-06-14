package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Registro de cada vez que un residente ve un anuncio.
 * Se inserta UNA sola vez por (anuncio, residente).
 */
@Entity
@Table(name = "anuncio_vistas",
       uniqueConstraints = @UniqueConstraint(columnNames = {"anuncio_id", "residente_id"}))
public class AnuncioVista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anuncio_id", nullable = false)
    private Long anuncioId;

    @Column(name = "residente_id", nullable = false)
    private Long residenteId;

    @Column(name = "residente_nombre", length = 150)
    private String residenteNombre;

    @CreationTimestamp
    @Column(name = "visto_en", nullable = false, updatable = false)
    private Instant vistoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAnuncioId() { return anuncioId; }
    public void setAnuncioId(Long anuncioId) { this.anuncioId = anuncioId; }

    public Long getResidenteId() { return residenteId; }
    public void setResidenteId(Long residenteId) { this.residenteId = residenteId; }

    public String getResidenteNombre() { return residenteNombre; }
    public void setResidenteNombre(String residenteNombre) { this.residenteNombre = residenteNombre; }

    public Instant getVistoEn() { return vistoEn; }
}
