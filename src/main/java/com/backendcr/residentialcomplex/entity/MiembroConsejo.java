package com.backendcr.residentialcomplex.entity;

import java.time.LocalDate;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.backendcr.residentialcomplex.entity.enums.CargoConsejo;

import jakarta.persistence.*;

/**
 * Membresía activa de un residente en el consejo comunal del tenant.
 * Un mismo usuario puede ser PROPIETARIO o INQUILINO y a la vez
 * tener un registro activo aquí (rol superpuesto).
 *
 * Vive dentro del schema tenant (no en public).
 */
@Entity
@Table(name = "miembro_consejo")
public class MiembroConsejo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cargo", nullable = false, length = 20)
    private CargoConsejo cargo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public CargoConsejo getCargo() { return cargo; }
    public void setCargo(CargoConsejo cargo) { this.cargo = cargo; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public Instant getCreadoEn() { return creadoEn; }
}
