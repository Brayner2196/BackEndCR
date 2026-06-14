package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.TipoCalculoMora;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "configuracion_mora")
public class ConfiguracionMora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "porcentaje_mensual", precision = 5, scale = 2)
    private BigDecimal porcentajeMensual;

    @Column(name = "dias_gracia")
    private int diasGracia = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_calculo", nullable = false, length = 20)
    private TipoCalculoMora tipoCalculo = TipoCalculoMora.PORCENTAJE;

    @Column(name = "monto_fijo", precision = 12, scale = 0)
    private BigDecimal montoFijo;

    private boolean activo = true;

    @Column(name = "fecha_vigencia", nullable = false)
    private LocalDate fechaVigencia;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getPorcentajeMensual() { return porcentajeMensual; }
    public void setPorcentajeMensual(BigDecimal p) { this.porcentajeMensual = p; }
    public int getDiasGracia() { return diasGracia; }
    public void setDiasGracia(int diasGracia) { this.diasGracia = diasGracia; }
    public TipoCalculoMora getTipoCalculo() { return tipoCalculo; }
    public void setTipoCalculo(TipoCalculoMora tipoCalculo) { this.tipoCalculo = tipoCalculo; }
    public BigDecimal getMontoFijo() { return montoFijo; }
    public void setMontoFijo(BigDecimal montoFijo) { this.montoFijo = montoFijo; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public LocalDate getFechaVigencia() { return fechaVigencia; }
    public void setFechaVigencia(LocalDate d) { this.fechaVigencia = d; }
    public Instant getCreadoEn() { return creadoEn; }
}
