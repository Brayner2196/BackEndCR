package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.TipoExcepcionZona;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "excepciones_zonas_comunes")
public class ExcepcionZonaComun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zona_comun_id", nullable = false)
    private Long zonaComunId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoExcepcionZona tipo;

    /** Solo requerido para APERTURA_ESPECIAL. */
    @Column(name = "hora_apertura")
    private LocalTime horaApertura;

    /** Solo requerido para APERTURA_ESPECIAL. */
    @Column(name = "hora_cierre")
    private LocalTime horaCierre;

    @Column(length = 300)
    private String motivo;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    // ── Getters / Setters ─────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getZonaComunId() { return zonaComunId; }
    public void setZonaComunId(Long zonaComunId) { this.zonaComunId = zonaComunId; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public TipoExcepcionZona getTipo() { return tipo; }
    public void setTipo(TipoExcepcionZona tipo) { this.tipo = tipo; }

    public LocalTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }

    public LocalTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Instant getCreadoEn() { return creadoEn; }
}
