package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.EstadoPQR;
import com.backendcr.residentialcomplex.entity.enums.TipoPQR;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "pqrs")
public class PQR {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoPQR tipo;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPQR estado = EstadoPQR.RADICADA;

    @Column(name = "residente_id", nullable = false)
    private Long residenteId;

    @Column(name = "propiedad_id")
    private Long propiedadId;

    @Column(name = "respuesta_admin", length = 500)
    private String respuestaAdmin;

    @Column(name = "respondido_por")
    private Long respondidoPor;

    @Column(name = "fecha_respuesta")
    private Instant fechaRespuesta;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TipoPQR getTipo() { return tipo; }
    public void setTipo(TipoPQR tipo) { this.tipo = tipo; }

    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public EstadoPQR getEstado() { return estado; }
    public void setEstado(EstadoPQR estado) { this.estado = estado; }

    public Long getResidenteId() { return residenteId; }
    public void setResidenteId(Long residenteId) { this.residenteId = residenteId; }

    public Long getPropiedadId() { return propiedadId; }
    public void setPropiedadId(Long propiedadId) { this.propiedadId = propiedadId; }

    public String getRespuestaAdmin() { return respuestaAdmin; }
    public void setRespuestaAdmin(String respuestaAdmin) { this.respuestaAdmin = respuestaAdmin; }

    public Long getRespondidoPor() { return respondidoPor; }
    public void setRespondidoPor(Long respondidoPor) { this.respondidoPor = respondidoPor; }

    public Instant getFechaRespuesta() { return fechaRespuesta; }
    public void setFechaRespuesta(Instant fechaRespuesta) { this.fechaRespuesta = fechaRespuesta; }

    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
