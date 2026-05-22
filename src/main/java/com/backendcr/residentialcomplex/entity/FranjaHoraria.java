package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

/**
 * Franja horaria dentro de un HorarioGrupoZona.
 * Permite representar horarios partidos: ej 10:00-14:00 y 16:00-22:00.
 *
 * NOTA: grupo_id es gestionado exclusivamente por el @JoinColumn del padre
 * (HorarioGrupoZona). No se mapea aquí para evitar el conflicto de columna dual
 * que provoca el error "null value in column grupo_id".
 */
@Entity
@Table(name = "franjas_horarias")
public class FranjaHoraria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    private int orden = 0;

    // ── Getters / Setters ──────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
