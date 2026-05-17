package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.EstadoPeriodo;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "periodos_cobro")
public class PeriodoCobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int anio;

    @Column(nullable = false)
    private int mes;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "fecha_limite_pago", nullable = false)
    private LocalDate fechaLimitePago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPeriodo estado = EstadoPeriodo.ABIERTO;
    
    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }
    public int getMes() { return mes; }
    public void setMes(int mes) { this.mes = mes; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate d) { this.fechaInicio = d; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate d) { this.fechaFin = d; }
    public LocalDate getFechaLimitePago() { return fechaLimitePago; }
    public void setFechaLimitePago(LocalDate d) { this.fechaLimitePago = d; }
    public EstadoPeriodo getEstado() { return estado; }
    public void setEstado(EstadoPeriodo estado) { this.estado = estado; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
     public Long getCreadoPor() { return creadoPor; }
	 public void setCreadoPor(Long creadoPor) { this.creadoPor = creadoPor; }
}
