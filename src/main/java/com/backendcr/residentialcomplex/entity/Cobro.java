package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.ConceptoCobro;
import com.backendcr.residentialcomplex.entity.enums.EstadoCobro;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cobros")
public class Cobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "periodo_id", nullable = false)
    private Long periodoId;

    @Column(name = "propiedad_id", nullable = false)
    private Long propiedadId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConceptoCobro concepto = ConceptoCobro.ADMINISTRACION;

    @Column(length = 200)
    private String descripcion;

    @Column(name = "monto_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoBase;

    @Column(name = "monto_mora", precision = 12, scale = 2)
    private BigDecimal montoMora = BigDecimal.ZERO;

    @Column(name = "monto_total", precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDate fechaGeneracion;

    @Column(name = "fecha_limite_pago", nullable = false)
    private LocalDate fechaLimitePago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCobro estado = EstadoCobro.PENDIENTE;

    @Column(name = "exonerado_por")
    private Long exoneradoPor;

    @Column(name = "nota_exoneracion", length = 300)
    private String notaExoneracion;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPeriodoId() { return periodoId; }
    public void setPeriodoId(Long periodoId) { this.periodoId = periodoId; }
    public Long getPropiedadId() { return propiedadId; }
    public void setPropiedadId(Long propiedadId) { this.propiedadId = propiedadId; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public ConceptoCobro getConcepto() { return concepto; }
    public void setConcepto(ConceptoCobro concepto) { this.concepto = concepto; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getMontoBase() { return montoBase; }
    public void setMontoBase(BigDecimal montoBase) { this.montoBase = montoBase; }
    public BigDecimal getMontoMora() { return montoMora; }
    public void setMontoMora(BigDecimal montoMora) { this.montoMora = montoMora; }
    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }
    public LocalDate getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDate d) { this.fechaGeneracion = d; }
    public LocalDate getFechaLimitePago() { return fechaLimitePago; }
    public void setFechaLimitePago(LocalDate d) { this.fechaLimitePago = d; }
    public EstadoCobro getEstado() { return estado; }
    public void setEstado(EstadoCobro estado) { this.estado = estado; }
    public Long getExoneradoPor() { return exoneradoPor; }
    public void setExoneradoPor(Long exoneradoPor) { this.exoneradoPor = exoneradoPor; }
    public String getNotaExoneracion() { return notaExoneracion; }
    public void setNotaExoneracion(String nota) { this.notaExoneracion = nota; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
