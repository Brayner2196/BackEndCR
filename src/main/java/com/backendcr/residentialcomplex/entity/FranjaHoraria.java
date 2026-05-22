package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

/**
 * Franja horaria dentro de un HorarioGrupoZona.
 * FK owner del lado Many → Hibernate inserta grupo_id correctamente desde el inicio.
 */
@Entity
@Table(name = "franjas_horarias")
public class FranjaHoraria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private HorarioGrupoZona grupo;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    private int orden = 0;

    // ── Getters / Setters ──────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public HorarioGrupoZona getGrupo() { return grupo; }
    public void setGrupo(HorarioGrupoZona grupo) { this.grupo = grupo; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
