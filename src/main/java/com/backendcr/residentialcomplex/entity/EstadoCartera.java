package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Estado de cartera configurable por el conjunto (tenant). Define un nivel
 * financiero de una propiedad (al día, vencida, en mora, prejurídico…), su
 * severidad y las reglas/restricciones asociadas.
 *
 * Las reglas y restricciones se cargan por repositorio (mismo patrón que
 * ZonaComun ↔ HorarioGrupoZona), no por relación JPA.
 */
@Entity
@Table(name = "estados_cartera")
public class EstadoCartera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Slug único dentro del tenant: AL_DIA, VENCIDA, MORA, COBRO_PREJURIDICO… */
    @Column(nullable = false, length = 40, unique = true)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    /** Mayor = más grave. Gana el estado más severo cuyas reglas se cumplan. */
    @Column(nullable = false)
    private int severidad = 0;

    /** Color hex para badges en la UI (ej. #A34A4A). */
    @Column(length = 9)
    private String color;

    /** true para el estado base "al día"; false para los negativos. */
    @Column(name = "es_positivo", nullable = false)
    private boolean esPositivo = false;

    @Column(nullable = false)
    private boolean activo = true;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getSeveridad() { return severidad; }
    public void setSeveridad(int severidad) { this.severidad = severidad; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isEsPositivo() { return esPositivo; }
    public void setEsPositivo(boolean esPositivo) { this.esPositivo = esPositivo; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
}
