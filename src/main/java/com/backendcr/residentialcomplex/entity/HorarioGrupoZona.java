package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Agrupa días de la semana que comparten las mismas franjas horarias.
 * Ej: "Días de semana" → LUNES,MARTES,MIERCOLES,JUEVES con franjas 08:00-18:00.
 * Un día sólo puede pertenecer a un grupo dentro de la misma zona.
 */
@Entity
@Table(name = "horarios_grupos_zona")
public class HorarioGrupoZona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zona_comun_id", nullable = false)
    private Long zonaComunId;

    /** Nombre descriptivo del grupo. Ej: "Días de semana", "Viernes", "Fin de semana" */
    @Column(nullable = false, length = 80)
    private String etiqueta;

    /**
     * Días incluidos en este grupo como CSV.
     * Valores: LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO
     */
    @Column(name = "dias", nullable = false, length = 100)
    private String dias;

    /** Nota interna visible sólo al admin. Ej: "Horario extendido para eventos" */
    @Column(length = 200)
    private String nota;

    @Column(nullable = false)
    private int orden = 0;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "grupo_id")
    @OrderBy("orden ASC")
    private List<FranjaHoraria> franjas = new ArrayList<>();

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
