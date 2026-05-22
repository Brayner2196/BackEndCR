package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Agrupa días de la semana que comparten las mismas franjas horarias.
 * Relación bidireccional con FranjaHoraria para que Hibernate inserte
 * el grupo_id correctamente en la FK (evita el error null en NOT NULL).
 */
@Entity
@Table(name = "horarios_grupos_zona")
public class HorarioGrupoZona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zona_comun_id", nullable = false)
    private Long zonaComunId;

    @Column(nullable = false, length = 80)
    private String etiqueta;

    @Column(name = "dias", nullable = false, length = 100)
    private String dias;

    @Column(length = 200)
    private String nota;

    @Column(nullable = false)
    private int orden = 0;

    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orden ASC")
    private List<FranjaHoraria> franjas = new ArrayList<>();

    // ── Helpers ────────────────────────────────────────────────

    /** Agrega una franja manteniendo la referencia bidireccional. */
    public void addFranja(FranjaHoraria franja) {
        franja.setGrupo(this);
        franjas.add(franja);
    }

    // ── Getters / Setters ──────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getZonaComunId() { return zonaComunId; }
    public void setZonaComunId(Long zonaComunId) { this.zonaComunId = zonaComunId; }

    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }

    public String getDias() { return dias; }
    public void setDias(String dias) { this.dias = dias; }

    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }

    public List<FranjaHoraria> getFranjas() { return franjas; }
    public void setFranjas(List<FranjaHoraria> franjas) { this.franjas = franjas; }
}
