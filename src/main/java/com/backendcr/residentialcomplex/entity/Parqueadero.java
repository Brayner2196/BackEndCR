package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.ModeloParqueaderoPrivado;
import com.backendcr.residentialcomplex.entity.enums.TipoParqueadero;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "parqueaderos",
       uniqueConstraints = @UniqueConstraint(columnNames = "identificador"))
public class Parqueadero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String identificador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoParqueadero tipo;

    /**
     * Solo aplica a parqueaderos PRIVADOS.
     * INDEPENDIENTE → es una propiedad facturable propia (ver propiedadParqueaderoId).
     * ACCESORIO     → complemento de un apartamento (ver propiedadId).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "modelo_propiedad", length = 20)
    private ModeloParqueaderoPrivado modeloPropiedad;

    /**
     * ACCESORIO: ID del apartamento al que pertenece este parqueadero.
     * INDEPENDIENTE: puede ser null o el ID de un apartamento relacionado (opcional).
     */
    @Column(name = "propiedad_id")
    private Long propiedadId;

    /**
     * INDEPENDIENTE únicamente: ID de la Propiedad de tipo parqueadero que representa
     * este spot físico en el árbol de tipos de propiedad.
     */
    @Column(name = "propiedad_parqueadero_id")
    private Long propiedadParqueaderoId;

    // Vehículo actualmente asignado (nullable)
    @Column(name = "vehiculo_id")
    private Long vehiculoId;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    // ── Getters / Setters ─────────────────────────────────────

    public Long getId() { return id; }

    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }

    public TipoParqueadero getTipo() { return tipo; }
    public void setTipo(TipoParqueadero tipo) { this.tipo = tipo; }

    public ModeloParqueaderoPrivado getModeloPropiedad() { return modeloPropiedad; }
    public void setModeloPropiedad(ModeloParqueaderoPrivado modeloPropiedad) { this.modeloPropiedad = modeloPropiedad; }

    public Long getPropiedadId() { return propiedadId; }
    public void setPropiedadId(Long propiedadId) { this.propiedadId = propiedadId; }

    public Long getPropiedadParqueaderoId() { return propiedadParqueaderoId; }
    public void setPropiedadParqueaderoId(Long propiedadParqueaderoId) { this.propiedadParqueaderoId = propiedadParqueaderoId; }

    public Long getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(Long vehiculoId) { this.vehiculoId = vehiculoId; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
}
