package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.EstadoVisita;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Visita pre-registrada por un residente. El campo {@code codigo} es el contenido
 * del QR que el vigilante valida en portería.
 */
@Entity
@Table(name = "visitas",
       indexes = @Index(name = "idx_visita_codigo", columnList = "codigo", unique = true))
public class Visita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16, unique = true)
    private String codigo;

    @Column(name = "nombre_visitante", nullable = false, length = 120)
    private String nombreVisitante;

    @Column(length = 30)
    private String documento;

    @Column(length = 15)
    private String placa;

    @Column(length = 200)
    private String motivo;

    @Column(name = "propiedad_id", nullable = false)
    private Long propiedadId;

    @Column(name = "residente_id", nullable = false)
    private Long residenteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoVisita estado = EstadoVisita.PENDIENTE;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "ingreso_en")
    private Instant ingresoEn;

    @Column(name = "validada_por")
    private Long validadaPor;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    // ── Getters / Setters ─────────────────────────────────────

    public Long getId() { return id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombreVisitante() { return nombreVisitante; }
    public void setNombreVisitante(String nombreVisitante) { this.nombreVisitante = nombreVisitante; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Long getPropiedadId() { return propiedadId; }
    public void setPropiedadId(Long propiedadId) { this.propiedadId = propiedadId; }

    public Long getResidenteId() { return residenteId; }
    public void setResidenteId(Long residenteId) { this.residenteId = residenteId; }

    public EstadoVisita getEstado() { return estado; }
    public void setEstado(EstadoVisita estado) { this.estado = estado; }

    public Instant getExpiraEn() { return expiraEn; }
    public void setExpiraEn(Instant expiraEn) { this.expiraEn = expiraEn; }

    public Instant getIngresoEn() { return ingresoEn; }
    public void setIngresoEn(Instant ingresoEn) { this.ingresoEn = ingresoEn; }

    public Long getValidadaPor() { return validadaPor; }
    public void setValidadaPor(Long validadaPor) { this.validadaPor = validadaPor; }

    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
