package com.backendcr.residentialcomplex.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.backendcr.residentialcomplex.entity.enums.RolPropiedad;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario_propiedades",
       uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "propiedad_id"}))
public class UsuarioPropiedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "propiedad_id", nullable = false)
    private Long propiedadId;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", length = 20)
    private RolPropiedad rol;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Long getPropiedadId() { return propiedadId; }
    public void setPropiedadId(Long propiedadId) { this.propiedadId = propiedadId; }

    public boolean isEsPrincipal() { return esPrincipal; }
    public void setEsPrincipal(boolean esPrincipal) { this.esPrincipal = esPrincipal; }

    public RolPropiedad getRol() { return rol; }
    public void setRol(RolPropiedad rol) { this.rol = rol; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
}
