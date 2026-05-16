package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "zonas_comunes")
public class ZonaComun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Integer capacidad = 0;

    @Column(nullable = false)
    private boolean activa = true;

    // ── Horario estándar ─────────────────────────
    @Column(name = "hora_apertura")
    private LocalTime horaApertura;

    @Column(name = "hora_cierre")
    private LocalTime horaCierre;

    /**
     * Días disponibles como cadena CSV (LUNES,MARTES,...).
     * Null/vacío = todos los días.
     */
    @Column(name = "dias_disponibles", length = 100)
    private String diasDisponibles;

    // ── Reglas de duración ───────────────────────
    /** Duración mínima de reserva en minutos. Null = sin restricción. */
    @Column(name = "duracion_min_minutos")
    private Integer duracionMinMinutos;

    /** Duración máxima de reserva en minutos. Null = sin restricción. */
    @Column(name = "duracion_max_minutos")
    private Integer duracionMaxMinutos;

    // ── Reglas de anticipación ───────────────────
    /** Mínimo de días de anticipación para reservar. Null = sin restricción. */
    @Column(name = "anticipacion_min_dias")
    private Integer anticipacionMinDias;

    /** Máximo de días de anticipación para reservar. Null = sin restricción. */
    @Column(name = "anticipacion_max_dias")
    private Integer anticipacionMaxDias;

    // ── Aprobación ───────────────────────────────
    /** Si false, la reserva se aprueba automáticamente al crearla. */
    @Column(name = "requiere_aprobacion", nullable = false)
    private boolean requiereAprobacion = true;

    // ── Suspensión ───────────────────────────────
    @Column(nullable = false)
    private boolean suspendida = false;

    @Column(name = "motivo_suspension", length = 300)
    private String motivoSuspension;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    // ── Getters / Setters ─────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    public LocalTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }

    public LocalTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }

    public String getDiasDisponibles() { return diasDisponibles; }
    public void setDiasDisponibles(String diasDisponibles) { this.diasDisponibles = diasDisponibles; }

    public Integer getDuracionMinMinutos() { return duracionMinMinutos; }
    public void setDuracionMinMinutos(Integer duracionMinMinutos) { this.duracionMinMinutos = duracionMinMinutos; }

    public Integer getDuracionMaxMinutos() { return duracionMaxMinutos; }
    public void setDuracionMaxMinutos(Integer duracionMaxMinutos) { this.duracionMaxMinutos = duracionMaxMinutos; }

    public Integer getAnticipacionMinDias() { return anticipacionMinDias; }
    public void setAnticipacionMinDias(Integer anticipacionMinDias) { this.anticipacionMinDias = anticipacionMinDias; }

    public Integer getAnticipacionMaxDias() { return anticipacionMaxDias; }
    public void setAnticipacionMaxDias(Integer anticipacionMaxDias) { this.anticipacionMaxDias = anticipacionMaxDias; }

    public boolean isRequiereAprobacion() { return requiereAprobacion; }
    public void setRequiereAprobacion(boolean requiereAprobacion) { this.requiereAprobacion = requiereAprobacion; }

    public boolean isSuspendida() { return suspendida; }
    public void setSuspendida(boolean suspendida) { this.suspendida = suspendida; }

    public String getMotivoSuspension() { return motivoSuspension; }
    public void setMotivoSuspension(String motivoSuspension) { this.motivoSuspension = motivoSuspension; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
}
