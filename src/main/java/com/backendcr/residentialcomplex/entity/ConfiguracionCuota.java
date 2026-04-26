package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.Periodicidad;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_cuotas")
public class ConfiguracionCuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_propiedad_id")
    private Long tipoPropiedadId;

    @Column(name = "propiedad_id")
    private Long propiedadId;

    /** Rango de número de propiedad (ambos inclusive). Null = aplica a todo el tipo. */
    @Column(name = "numero_desde")
    private Integer numeroDesde;

    @Column(name = "numero_hasta")
    private Integer numeroHasta;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Periodicidad periodicidad = Periodicidad.MENSUAL;

    @Column(name = "fecha_vigencia_desde", nullable = false)
    private LocalDate fechaVigenciaDesde;

    private boolean activo = true;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTipoPropiedadId() { return tipoPropiedadId; }
    public void setTipoPropiedadId(Long tipoPropiedadId) { this.tipoPropiedadId = tipoPropiedadId; }
    public Long getPropiedadId() { return propiedadId; }
    public void setPropiedadId(Long propiedadId) { this.propiedadId = propiedadId; }
    public Integer getNumeroDesde() { return numeroDesde; }
    public void setNumeroDesde(Integer numeroDesde) { this.numeroDesde = numeroDesde; }
    public Integer getNumeroHasta() { return numeroHasta; }
    public void setNumeroHasta(Integer numeroHasta) { this.numeroHasta = numeroHasta; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public Periodicidad getPeriodicidad() { return periodicidad; }
    public void setPeriodicidad(Periodicidad periodicidad) { this.periodicidad = periodicidad; }
    public LocalDate getFechaVigenciaDesde() { return fechaVigenciaDesde; }
    public void setFechaVigenciaDesde(LocalDate d) { this.fechaVigenciaDesde = d; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
