package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.EstadoVotacion;
import com.backendcr.residentialcomplex.entity.enums.TipoVotacion;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "votaciones")
public class Votacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_votacion", nullable = false, length = 30)
    private TipoVotacion tipoVotacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVotacion estado = EstadoVotacion.BORRADOR;

    /** Valor máximo para tipo ESCALA_NUMERICA (ej. 5 = del 1 al 5) */
    @Column(name = "escala_max")
    private Integer escalaMax;

    /** Si true, cualquier residente puede ver quiénes votaron y sus respuestas */
    @Column(name = "mostrar_votantes", nullable = false)
    private boolean mostrarVotantes = false;

    /** Si true, un residente puede cambiar su voto mientras la votación esté ABIERTA */
    @Column(name = "permite_cambiar_voto", nullable = false)
    private boolean permiteCambiarVoto = false;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public TipoVotacion getTipoVotacion() { return tipoVotacion; }
    public void setTipoVotacion(TipoVotacion tipoVotacion) { this.tipoVotacion = tipoVotacion; }

    public EstadoVotacion getEstado() { return estado; }
    public void setEstado(EstadoVotacion estado) { this.estado = estado; }

    public Integer getEscalaMax() { return escalaMax; }
    public void setEscalaMax(Integer escalaMax) { this.escalaMax = escalaMax; }

    public boolean isMostrarVotantes() { return mostrarVotantes; }
    public void setMostrarVotantes(boolean mostrarVotantes) { this.mostrarVotantes = mostrarVotantes; }

    public boolean isPermiteCambiarVoto() { return permiteCambiarVoto; }
    public void setPermiteCambiarVoto(boolean permiteCambiarVoto) { this.permiteCambiarVoto = permiteCambiarVoto; }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }

    public Long getCreadoPor() { return creadoPor; }
    public void setCreadoPor(Long creadoPor) { this.creadoPor = creadoPor; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
