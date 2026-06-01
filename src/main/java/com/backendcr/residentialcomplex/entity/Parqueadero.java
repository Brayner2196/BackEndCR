package com.backendcr.residentialcomplex.entity;

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

    // Solo aplica si tipo = PRIVADO
    @Column(name = "propiedad_id")
    private Long propiedadId;

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

    public Long getPropiedadId() { return propiedadId; }
    public void setPropiedadId(Long propiedadId) { this.propiedadId = propiedadId; }

    public Long getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(Long vehiculoId) { this.vehiculoId = vehiculoId; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
}
