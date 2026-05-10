package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;

/**
 * Opciones predefinidas para tipos OPCION_UNICA y OPCION_MULTIPLE.
 * Para ESCALA_NUMERICA y TEXTO_LIBRE no se crean opciones.
 */
@Entity
@Table(name = "opciones_votacion")
public class OpcionVotacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "votacion_id", nullable = false)
    private Long votacionId;

    @Column(nullable = false, length = 300)
    private String texto;

    @Column(nullable = false)
    private int orden = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVotacionId() { return votacionId; }
    public void setVotacionId(Long votacionId) { this.votacionId = votacionId; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
