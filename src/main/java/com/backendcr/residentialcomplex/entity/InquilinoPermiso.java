package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.PermisoInquilino;
import jakarta.persistence.*;

/**
 * Permiso otorgado por un propietario a uno de sus inquilinos.
 * La tabla vive en el schema del tenant (sin anotación de schema explícita).
 */
@Entity
@Table(name = "inquilino_permisos",
       uniqueConstraints = @UniqueConstraint(columnNames = {"inquilino_id", "permiso"}))
public class InquilinoPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inquilino_id", nullable = false)
    private Long inquilinoId;

    @Column(name = "propietario_id", nullable = false)
    private Long propietarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PermisoInquilino permiso;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInquilinoId() { return inquilinoId; }
    public void setInquilinoId(Long inquilinoId) { this.inquilinoId = inquilinoId; }

    public Long getPropietarioId() { return propietarioId; }
    public void setPropietarioId(Long propietarioId) { this.propietarioId = propietarioId; }

    public PermisoInquilino getPermiso() { return permiso; }
    public void setPermiso(PermisoInquilino permiso) { this.permiso = permiso; }
}
