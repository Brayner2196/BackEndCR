package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tipos_propiedad")
public class TipoPropiedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "parent_id")
    private Long parentId;
    
    @Column(name="es_facturable", nullable = false)
    private boolean esFacturable = true;

    @Column(name = "es_parqueadero", nullable = false)
    private boolean esParqueadero = false;

    @Column(nullable = false)
    private int orden = 0;

    @Column(nullable = false)
    private boolean activo = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public boolean isEsFacturable() { return esFacturable; }
    public void setEsFacturable(boolean esFacturable) { this.esFacturable = esFacturable; }

    public boolean isEsParqueadero() { return esParqueadero; }
    public void setEsParqueadero(boolean esParqueadero) { this.esParqueadero = esParqueadero; }
}
